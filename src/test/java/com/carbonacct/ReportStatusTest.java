package com.carbonacct;

import com.carbonacct.common.enums.ReportStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportStatusTest {

    @Test
    void testValidTransitions() {
        assertTrue(ReportStatus.DRAFT.canTransitionTo(ReportStatus.PENDING_REVIEW));
        assertTrue(ReportStatus.PENDING_REVIEW.canTransitionTo(ReportStatus.PUBLISHED));
        assertTrue(ReportStatus.PENDING_REVIEW.canTransitionTo(ReportStatus.DRAFT));
        assertTrue(ReportStatus.PUBLISHED.canTransitionTo(ReportStatus.CORRECTED));
    }

    @Test
    void testInvalidTransitions() {
        assertFalse(ReportStatus.DRAFT.canTransitionTo(ReportStatus.PUBLISHED));
        assertFalse(ReportStatus.DRAFT.canTransitionTo(ReportStatus.CORRECTED));
        assertFalse(ReportStatus.PUBLISHED.canTransitionTo(ReportStatus.DRAFT));
        assertFalse(ReportStatus.PUBLISHED.canTransitionTo(ReportStatus.PENDING_REVIEW));
        assertFalse(ReportStatus.CORRECTED.canTransitionTo(ReportStatus.DRAFT));
        assertFalse(ReportStatus.CORRECTED.canTransitionTo(ReportStatus.PENDING_REVIEW));
        assertFalse(ReportStatus.CORRECTED.canTransitionTo(ReportStatus.PUBLISHED));
        assertFalse(ReportStatus.CORRECTED.canTransitionTo(ReportStatus.CORRECTED));
    }

    @Test
    void testGetDesc() {
        assertEquals("草稿", ReportStatus.DRAFT.getDesc());
        assertEquals("待复核", ReportStatus.PENDING_REVIEW.getDesc());
        assertEquals("已发布", ReportStatus.PUBLISHED.getDesc());
        assertEquals("已更正", ReportStatus.CORRECTED.getDesc());
    }

    @Test
    void testGetCode() {
        assertEquals(0, ReportStatus.DRAFT.getCode());
        assertEquals(1, ReportStatus.PENDING_REVIEW.getCode());
        assertEquals(2, ReportStatus.PUBLISHED.getCode());
        assertEquals(3, ReportStatus.CORRECTED.getCode());
    }
}
