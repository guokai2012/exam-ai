package com.exam.ai.question.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.question.dto.CreateQuestionCategoryRequest;
import com.exam.ai.question.dto.QuestionCategoryResponse;
import com.exam.ai.question.dto.QuestionResponse;
import com.exam.ai.question.dto.ReviewQuestionRequest;
import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "题库管理接口", description = "题目分类、题库查询、题目详情和题目审核确认")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping("/api/question-categories")
    @PreAuthorize("hasAuthority('question-category:list')")
    @Operation(summary = "题目分类列表", description = "查询当前可用的题目分类。")
    public ApiResponse<List<QuestionCategoryResponse>> categories() {
        return ApiResponse.ok(questionBankService.categories());
    }

    @PostMapping("/api/question-categories")
    @PreAuthorize("hasAuthority('question-category:create')")
    @Operation(summary = "新建题目分类", description = "教师创建题目分类。")
    public ApiResponse<QuestionCategoryResponse> createCategory(@Valid @RequestBody CreateQuestionCategoryRequest request,
                                                               @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(questionBankService.createCategory(request, principal));
    }

    @GetMapping("/api/questions")
    @PreAuthorize("hasAuthority('question:list')")
    @Operation(summary = "分页查询题目", description = "按分类、题型、状态和标签筛选题库题目。")
    public ApiResponse<IPage<QuestionResponse>> questions(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                          @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long size,
                                                          @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
                                                          @Parameter(description = "题型") @RequestParam(required = false) String questionType,
                                                          @Parameter(description = "题目状态") @RequestParam(required = false) String state,
                                                          @Parameter(description = "标签 ID") @RequestParam(required = false) Long tagId,
                                                          @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(questionBankService.listQuestions(page, size, categoryId, questionType, state, tagId, principal));
    }

    @GetMapping("/api/questions/{id}")
    @PreAuthorize("hasAuthority('question:detail')")
    @Operation(summary = "题目详情", description = "查询单道题目的完整内容、答案、解析和标签。")
    public ApiResponse<QuestionResponse> questionDetail(@Parameter(description = "题目 ID") @PathVariable Long id,
                                                        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(questionBankService.detail(id, principal));
    }

    @PostMapping("/api/questions/{id}/review")
    @PreAuthorize("hasAuthority('question:review')")
    @Operation(summary = "审核确认题目", description = "确认或驳回从文档解析得到的待确认题目。")
    public ApiResponse<QuestionResponse> review(@Parameter(description = "题目 ID") @PathVariable Long id,
                                                @Valid @RequestBody ReviewQuestionRequest request,
                                                @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(questionBankService.review(id, request, principal));
    }
}
