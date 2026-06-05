package com.exam.ai.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.question.entity.ExamQuestionBank;
import com.exam.ai.question.entity.ExamQuestionCategory;
import com.exam.ai.question.entity.ExamQuestionSource;
import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import com.exam.ai.question.mapper.ExamQuestionBankMapper;
import com.exam.ai.question.mapper.ExamQuestionCategoryMapper;
import com.exam.ai.question.mapper.ExamQuestionSourceMapper;
import com.exam.ai.question.mapper.ExamQuestionTagMapper;
import com.exam.ai.question.mapper.ExamQuestionTagRelationMapper;
import com.exam.ai.question.service.impl.QuestionBankServiceImpl;
import com.exam.ai.question.util.QuestionStemNormalizer;
import com.exam.ai.question.dto.ReviewQuestionRequest;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.service.NotificationService;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.util.CurrentUserUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private ExamQuestionCategoryMapper categoryMapper;
    @Mock
    private ExamQuestionBankMapper questionMapper;
    @Mock
    private ExamQuestionSourceMapper sourceMapper;
    @Mock
    private ExamQuestionTagMapper tagMapper;
    @Mock
    private ExamQuestionTagRelationMapper tagRelationMapper;
    @Mock
    private QuestionStemNormalizer stemNormalizer;
    @Mock
    private QuestionStateTransitionService stateTransitionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private QuestionBankServiceImpl questionBankService;

    @Test
    void shouldIncreaseRetryCountBeforeRetryTagging() {
        ExamQuestionBank question = new ExamQuestionBank();
        question.setId(1L);
        question.setState(QuestionState.TAG_FAILED.name());
        question.setTagRetryCount(2);
        when(stateTransitionService.transit(1L, QuestionState.TAG_FAILED.name(), QuestionEvent.RETRY_TAGGING))
                .thenReturn(QuestionState.TAG_PROCESSING);
        when(questionMapper.selectById(1L)).thenReturn(question);

        ExamQuestionBank result = questionBankService.startTagging(question);

        assertThat(result.getState()).isEqualTo(QuestionState.TAG_PROCESSING.name());
        assertThat(result.getTagRetryCount()).isEqualTo(3);
        verify(questionMapper).updateById(question);
    }

    @Test
    void shouldNotifyQuestionOwnerWhenRetryLimitReached() {
        ExamQuestionBank question = new ExamQuestionBank();
        question.setId(2L);
        question.setState(QuestionState.TAG_PROCESSING.name());
        question.setStem("Java 中 List 和 Set 的区别是什么？");
        question.setCreateId(9L);
        question.setTagRetryCount(3);
        when(questionMapper.selectById(2L)).thenReturn(question);
        when(userMapper.selectById(9L)).thenReturn(new SysUser());
        when(stateTransitionService.transit(2L, QuestionState.TAG_PROCESSING.name(), QuestionEvent.TAG_FAIL))
                .thenReturn(QuestionState.TAG_FAILED);

        questionBankService.tagFailed(2L, "AI 超时", 3);

        ArgumentCaptor<ExamQuestionBank> captor = ArgumentCaptor.forClass(ExamQuestionBank.class);
        verify(questionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getTagNotifiedAt()).isNotNull();
        assertThat(captor.getValue().getTagErrorMessage()).isEqualTo("AI 超时");
        verify(notificationService).create(
                9L,
                "题目 AI 标签分析失败",
                "题目 #2 在首次分析失败后已重试 3 次，仍未生成题型标签，请人工处理。题干：Java 中 List 和 Set 的区别是什么？",
                NotificationService.TYPE_AI_TAGGING_FAILED,
                NotificationService.BUSINESS_QUESTION,
                2L
        );
    }

    @Test
    void shouldSkipNotificationWhenQuestionOwnerMissing() {
        ExamQuestionBank question = new ExamQuestionBank();
        question.setId(3L);
        question.setState(QuestionState.TAG_PROCESSING.name());
        question.setStem("题干");
        question.setTagRetryCount(0);
        when(questionMapper.selectById(3L)).thenReturn(question);
        when(stateTransitionService.transit(3L, QuestionState.TAG_PROCESSING.name(), QuestionEvent.TAG_FAIL))
                .thenReturn(QuestionState.TAG_FAILED);

        questionBankService.tagFailed(3L, "AI API Key 未配置", 0);

        org.mockito.Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void shouldRejectReviewForOtherTeacherQuestion() {
        ExamQuestionBank question = new ExamQuestionBank();
        question.setId(4L);
        question.setCreateId(100L);
        when(questionMapper.selectById(4L)).thenReturn(question);

        UserPrincipal teacher = new UserPrincipal(200L, "teacher", "session", java.util.List.of("TEACHER"), java.util.List.of());

        assertThatThrownBy(() -> CurrentUserUtils.runAs(teacher,
                (Callable<?>) () -> questionBankService.review(4L, new ReviewQuestionRequest(true, null, "通过"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问");
    }

    @Test
    void shouldSaveSortedSourcePageNosWhenImportQuestion() throws Exception {
        AiQuestionItem item = new AiQuestionItem("SINGLE_CHOICE", "Java 是什么？", List.of("A. 语言"),
                "A", "解析", 2, BigDecimal.valueOf(0.9), "Java 基础");
        ExamQuestionCategory category = new ExamQuestionCategory();
        category.setId(11L);
        category.setCategoryName("Java 基础");
        when(categoryMapper.selectOne(any())).thenReturn(category);
        when(stemNormalizer.normalize("Java 是什么？")).thenReturn("Java 是什么？");
        when(stemNormalizer.hash("Java 是什么？")).thenReturn("hash");
        when(questionMapper.selectOne(any())).thenReturn(null);
        when(objectMapper.writeValueAsString(List.of("A. 语言"))).thenReturn("[\"A. 语言\"]");

        questionBankService.importQuestion(item, 1L, 2L, 100L, List.of(3, 1, 3, 2), 1, 9L);

        ArgumentCaptor<ExamQuestionSource> captor = ArgumentCaptor.forClass(ExamQuestionSource.class);
        verify(sourceMapper).insert(captor.capture());
        assertThat(captor.getValue().getSourcePageNos()).isEqualTo("1,2,3");
        assertThat(captor.getValue().getChunkId()).isEqualTo(100L);
    }
}

