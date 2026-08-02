# Finance Ledger Project - Product Definition & Development Plan

## Vision

Build a production-style personal finance backend using Spring Boot that
intelligently captures transactions from payment screenshots, receipts,
and manual entries while distinguishing expenses from debt-related money
movement.

This project is intended to be one of the primary resume/internship
projects and will prioritize clean architecture, scalability, and
interview-quality engineering decisions over simply implementing
features.

------------------------------------------------------------------------

# Product Goals

## Problems Solved

-   Track both cash and digital spending.
-   Keep receipts/payment screenshots permanently linked to
    transactions.
-   Separate expenses from lending/borrowing.
-   Maintain complete debt history with partial repayments.
-   Generate personalized monthly financial insights ("Money Wrapped").

------------------------------------------------------------------------

# Core Features

## Transaction Capture

1.  Manual Entry
2.  Payment Screenshot + OCR
3.  Receipt Scan + OCR
4.  Manual fallback after OCR

OCR always creates a draft transaction which the user confirms before
saving.

------------------------------------------------------------------------

## Transaction Types

-   Expense
-   Income
-   Money Lent
-   Money Borrowed
-   Repayment Received
-   Repayment Paid

Only Expense contributes to spending analytics.

------------------------------------------------------------------------

## Payment Methods

-   Cash
-   Digital

------------------------------------------------------------------------

## Entry Sources

-   Manual
-   Payment Screenshot
-   Receipt

------------------------------------------------------------------------

# Receipt Vault

Every uploaded document remains permanently attached to its transaction.

Future use cases:

-   Warranty
-   Returns
-   Reimbursements
-   Purchase history

------------------------------------------------------------------------

# Debt System

The system stores lending events separately from expenses.

Supports:

-   Multiple loans
-   Partial repayments
-   Outstanding balances
-   Debt history

Outstanding balance should be derived from transactions rather than
stored independently.

------------------------------------------------------------------------

# Monthly Money Wrapped

Examples:

-   Total spending
-   Cash vs Digital
-   Category distribution
-   Merchant analysis
-   Spending spikes
-   Small purchases accumulation
-   Debt statistics
-   Month-over-month comparison

Insight Engine ranks interesting observations and presents only the most
relevant ones.

------------------------------------------------------------------------

# Initial Domain Model

Entities:

-   User
-   Transaction
-   Category
-   Person
-   Debt
-   Attachment
-   ExtractionResult
-   MonthlySummary
-   Insight

Relationships:

User ├── Transactions ├── Categories ├── People └── Monthly Summaries

Transaction ├── Category ├── Attachment(s) ├── Person (optional) └──
Debt (optional)

Attachment └── ExtractionResult

Debt └── Repayment Transactions

------------------------------------------------------------------------

# Planned Tech Stack

Backend - Java 21 - Spring Boot

Persistence - PostgreSQL - Spring Data JPA - Hibernate - Flyway

Security - Spring Security - JWT

Documentation - Swagger/OpenAPI

Storage - Local File Storage (development) - Object Storage (future)

OCR - External OCR / AI service

Testing - JUnit - Mockito - Spring Boot Test

Build Tool - Maven

Version Control - Git + GitHub

------------------------------------------------------------------------

# High-Level Architecture

Client

↓

REST API

↓

Spring Boot Backend

↓

Business Services

↓

Repositories (JPA)

↓

PostgreSQL

Parallel Services

-   File Storage
-   OCR
-   Analytics
-   Insight Engine

------------------------------------------------------------------------

# Development Phases

## Phase 0 -- Design

-   Product Definition
-   Domain Model
-   API Design
-   Database Design
-   Architecture

## Phase 1

Spring Boot Setup

## Phase 2

Transaction Module

## Phase 3

Persistence (PostgreSQL + JPA)

## Phase 4

Authentication (JWT)

## Phase 5

Debt System

## Phase 6

Receipt Vault

## Phase 7

OCR Pipeline

## Phase 8

Analytics

## Phase 9

Money Wrapped

## Phase 10

Testing

## Phase 11

Deployment

------------------------------------------------------------------------

# Development Principles

-   Build a modular monolith.
-   Learn every Spring concept while implementing it.
-   Prefer understanding over AI-generated code.
-   Keep OCR separate from financial logic.
-   Use transactions as the source of truth.
-   Design before implementation.
-   Write code that can be confidently explained during internship
    interviews.
