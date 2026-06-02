package com.exam.ai.question.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.question.dto.ReviewQuestionRequest;
import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.question.vo.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库题目 Controller，负责题目分页、详情和审核确认。
 */
@RestController
@RequestMapping("/api/questions")
@Tag(name = "题库管理接口", description = "题库查询、题目详情和题目审核确认")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    /**
     * 构造 QuestionBankController 实例并注入运行所需依赖。
     * @param questionBankService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param categoryId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param questionType 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param state 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param tagId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('question:list')")
    @Operation(summary = "分页查询题目", description = "按分类、题型、状态和标签筛选题库题目。")
    public ApiResponse<IPage<QuestionResponse>> questions(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                          @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long size,
                                                          @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
                                                          @Parameter(description = "题型") @RequestParam(required = false) String questionType,
                                                          @Parameter(description = "题目状态") @RequestParam(required = false) String state,
                                                          @Parameter(description = "标签 ID") @RequestParam(required = false) Long tagId) {
        return ApiResponse.ok(questionBankService.listQuestions(page, size, categoryId, questionType, state, tagId));
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('question:detail')")
    @Operation(summary = "题目详情", description = "查询单道题目的完整内容、答案、解析和标签。")
    public ApiResponse<QuestionResponse> questionDetail(@Parameter(description = "题目 ID") @PathVariable Long id) {
        return ApiResponse.ok(questionBankService.detailForCurrentUser(id));
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('question:review')")
    @Operation(summary = "审核确认题目", description = "确认或驳回从文档解析得到的待确认题目。")
    public ApiResponse<QuestionResponse> review(@Parameter(description = "题目 ID") @PathVariable Long id,
                                                @Valid @RequestBody ReviewQuestionRequest request) {
        return ApiResponse.ok(questionBankService.review(id, request));
    }
}
