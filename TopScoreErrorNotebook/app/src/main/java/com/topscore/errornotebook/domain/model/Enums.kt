package com.topscore.errornotebook.domain.model

/**
 * 学段枚举
 */
enum class SubjectStage {
    PRIMARY,   // 小学
    MIDDLE,    // 初中
    HIGH       // 高中
}

/**
 * 错因枚举
 */
enum class ErrorReason {
    MISREAD,        // 审题不清
    CALC_ERROR,     // 计算错误
    CONCEPT_UNCLEAR, // 概念模糊
    KNOWLEDGE_GAP,  // 知识点遗漏
    CARELESS,       // 粗心大意
    OTHER           // 其他
}

/**
 * 题型枚举
 */
enum class QuestionType {
    CHOICE,         // 选择题
    FILL_BLANK,     // 填空题
    SOLUTION,       // 解答题
    PROOF,          // 证明题
    OTHER           // 其他
}

/**
 * 错题状态
 */
enum class QuestionStatus {
    ACTIVE,     // 活跃（未归档）
    ARCHIVED,   // 已归档
    DELETED     // 已删除
}

/**
 * 同步状态
 */
enum class SyncStatus {
    PENDING,    // 待同步
    SYNCED,     // 已同步
    FAILED      // 同步失败
}