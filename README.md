# FinanceFlow

FinanceFlow is a Personal Finance Management System built with Java Spring Boot.

## Tech Stack

- Java 21
- Spring Boot 3
- PostgreSQL
- Spring Data JPA
- Flyway
- Validation
- Lombok
- MapStruct
- Swagger/OpenAPI

## Features

- User Management
- Bank Account Management
- Transaction Management
- CSV Import
- Auto Categorization Rules
- Dashboard & Reports

## Database

- PostgreSQL
- Flyway Migration

# 💰 FinanceFlow

FinanceFlow is a full-stack personal finance management application that helps users manage their daily income, expenses, bank accounts, and financial reports in one place.

The project was built to practice full-stack development using **Spring Boot** for the backend and **React** for the frontend while following a clean architecture and RESTful API design.

---

## ✨ Features

### 🔐 Authentication

- User registration
- User login
- JWT authentication
- Password encryption using BCrypt
- Protected API endpoints

---

### 📊 Dashboard

The dashboard provides a quick overview of a user's financial situation.

It includes:

- Current balance
- Total income
- Total expenses
- Net balance
- Recent transactions
- Expense by category
- Income vs Expense statistics

---

### 💳 Account Management

Manage different financial accounts such as:

- Bank Account
- Cash
- Credit Card
- E-Wallet

Users can:

- Create accounts
- Edit accounts
- Delete accounts

---

### 🏷 Category Management

Users can organize transactions by categories.

Examples:

- Salary
- Food
- Shopping
- Entertainment
- Transportation

Each category supports:

- Custom color
- Icon
- Priority
- Income or Expense type

---

### 💸 Transaction Management

Users can record financial activities by:

- Adding transactions
- Editing transactions
- Deleting transactions

Each transaction contains:

- Amount
- Description
- Category
- Account
- Transaction date
- Income or Expense type

---

### 🤖 Smart Rules

FinanceFlow supports automatic transaction categorization.

Example:

If a transaction description contains:

```
Starbucks
```

It can automatically be assigned to:

```
Food & Drink
```

This helps reduce manual work when importing bank statements.

---

### 📂 CSV Import

Users can import transactions from CSV files.

The application:

- Reads the CSV file
- Processes each row
- Saves valid transactions
- Reports failed rows
- Stores import history

---

## 🏗 Project Architecture

The project is divided into two independent applications.

### Backend

Responsible for:

- REST API
- Authentication
- Business logic
- Database access
- CSV processing
- Security

Built with:

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Flyway
- PostgreSQL

---

### Frontend

Responsible for:

- User Interface
- Dashboard
- Forms
- API communication
- Charts
- State management

Built with:

- React
- TypeScript
- Vite
- TailwindCSS
- TanStack Query
- React Hook Form
- Axios
- Recharts

---

## 🚀 Running the Project

### Clone the repository

```bash
git clone https://github.com/thanhhuy2310/FinanceFlowProject.git
```

---

### Backend

```bash
cd backend

mvn clean install

mvn spring-boot:run
```

Backend will run at

```
http://localhost:8080
```

---

### Frontend

```bash
cd frontend

npm install

npm run dev
```

Frontend will run at

```
http://localhost:5173
```

---

## ⚙ Environment Variables

### Backend

Create a `.env` file.

```env
DATABASE_URL=

DATABASE_USERNAME=

DATABASE_PASSWORD=

JWT_SECRET=

JWT_EXPIRATION=

FRONTEND_URL=
```

---

### Frontend

Create a `.env.local` file.

```env
VITE_API_URL=http://localhost:8080
```

For production:

```env
VITE_API_URL=https://financeflow-api-k56n.onrender.com
```

---

## 📖 API Documentation

Swagger UI

```
https://financeflow-api-k56n.onrender.com/swagger-ui/index.html
```

Some available endpoints include:

```
POST /api/auth/login

POST /api/auth/register

GET /api/dashboard

GET /api/accounts

GET /api/categories

GET /api/providers

GET /api/transactions

GET /api/rules

GET /api/import-batches
```

---

## ☁ Deployment

Frontend

```
Vercel
```

Backend

```
Render
```

Database

```
Neon PostgreSQL
```

---

## 🔒 Security

FinanceFlow uses modern authentication and security practices.

- JWT Authentication
- BCrypt Password Encryption
- Stateless Authentication
- Spring Security
- CORS Configuration
- Role-based Authorization

---

## 📅 Future Improvements

Planned features include:

- Forgot Password
- Email Verification
- Budget Planning
- Savings Goals
- Recurring Transactions
- Export Reports (PDF / Excel)
- Dark Mode
- Mobile Responsive Improvements
- Multi-language Support

---
