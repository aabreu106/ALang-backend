package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "note_tags")
@Data
public class NoteTag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(name = "tag_category", nullable = false, length = 50)
    private String tagCategory;

    @Column(name = "tag_value", nullable = false, length = 100)
    private String tagValue;
}
