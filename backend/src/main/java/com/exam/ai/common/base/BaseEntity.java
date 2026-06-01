package com.exam.ai.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据库实体公共基类，统一承载主键、创建审计、更新审计和逻辑删除字段。
 *
 * <p>所有带 {@code @TableName} 的业务实体都必须继承该类，避免每张表重复声明公共字段。
 * {@code deleted} 由 MyBatis-Plus 全局逻辑删除配置维护：未删除为 {@code 0}，删除时写入当前记录
 * {@code id}，从而配合包含 {@code deleted} 的唯一索引解决逻辑删除后的唯一值复用问题。</p>
 */
@Getter
@Setter
@EqualsAndHashCode
public abstract class BaseEntity {

    /**
     * 数据库自增主键，所有业务表统一使用 MyBatis-Plus 自增 ID 策略。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建人 ID，由 MyBatis-Plus 自动填充器从当前用户上下文写入。
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createId;

    /**
     * 创建时间，由 MyBatis-Plus 自动填充器在插入数据时写入。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人 ID，由 MyBatis-Plus 自动填充器在插入和更新数据时写入。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateId;

    /**
     * 更新时间，由 MyBatis-Plus 自动填充器在插入和更新数据时写入。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。未删除固定为 0，删除时由 MyBatis-Plus 全局逻辑删除配置写入当前记录 ID。
     */
    private Long deleted = 0L;
}
