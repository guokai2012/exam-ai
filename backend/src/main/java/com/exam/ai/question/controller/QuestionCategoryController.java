package com.exam.ai.question.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.question.dto.CreateQuestionCategoryRequest;
import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.question.vo.QuestionCategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目分类 Controller，负责题目分类查询和新增。
 */
@RestController
@RequestMapping("/api/question-categories")
@Tag(name = "题目分类接口", description = "题目分类查询和维护")
public class QuestionCategoryController {

    private final QuestionBankService questionBankService;

    /**
     * 构造题目分类 Controller。
     *
     * @param questionBankService 题库业务服务，用于读取和创建分类。
     */
    public QuestionCategoryController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    /**
     * 查询当前可用的题目分类。
     *
     * @return 题目分类列表。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('question-category:list')")
    @Operation(summary = "题目分类列表", description = "查询当前可用的题目分类。")
    public ApiResponse<List<QuestionCategoryResponse>> categories() {
        return ApiResponse.ok(questionBankService.categories());
    }

    /**
     * 创建题目分类。
     *
     * @param request 分类创建请求。
     * @return 创建后的题目分类。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('question-category:create')")
    @Operation(summary = "新建题目分类", description = "教师创建题目分类。")
    public ApiResponse<QuestionCategoryResponse> createCategory(@Valid @RequestBody CreateQuestionCategoryRequest request) {
        return ApiResponse.ok(questionBankService.createCategory(request));
    }
}
