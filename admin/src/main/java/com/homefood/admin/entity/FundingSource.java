package com.homefood.admin.entity;

/** Whose money paid for an {@link Expense} - not to be confused with {@link ExpenseCategory},
 * which tracks what the expense was FOR (ingredient purchase vs. other), not where the money
 * came from. */
public enum FundingSource {
    INVESTOR,
    WORKING_CAPITAL
}
