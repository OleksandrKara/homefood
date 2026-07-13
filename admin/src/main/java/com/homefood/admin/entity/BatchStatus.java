package com.homefood.admin.entity;

public enum BatchStatus {
    /** Announced ahead of time; doesn't move stock yet. */
    PLANNED,
    /** Actually produced; stock effects (consume ingredients, add finished goods) already applied. */
    DONE
}
