package com.umc.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.time.LocalDateTime;

/** 기존 MySQL의 DEFAULT / ON UPDATE 시간 정책을 유지하고, 저장 후 DB 값을 읽습니다. */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
