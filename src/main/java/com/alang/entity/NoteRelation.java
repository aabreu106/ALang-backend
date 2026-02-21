package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "note_relations")
@Data
public class NoteRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "source_note_id", nullable = false)
    private Note sourceNote;

    @ManyToOne
    @JoinColumn(name = "target_note_id", nullable = false)
    private Note targetNote;

    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;
}
