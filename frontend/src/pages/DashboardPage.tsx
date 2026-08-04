import { useQuery } from "@tanstack/react-query";
import {
  ArrowDownRight,
  ArrowUpRight,
  CreditCard,
  Plus,
  WalletCards,
} from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { financeApi } from "../api/finance";
import { PageHeader } from "../components/ui/PageHeader";
import { EmptyState, ErrorState } from "../components/ui/States";
import { formatCurrency, formatDate } from "../utils/format";

const CHART_COLORS = {
  income: "#16a34a",
  expense: "#ef4444",
};

const DESCRIPTION = "Your financial position at a glance.";

type MetricTone = "primary" | "income" | "expense" | "info";

interface MetricCardProps {
  label: string;
  value: number;
  trend?: "income" | "expense";
  tone: MetricTone;
  icon: typeof WalletCards;
}

function MetricCard({ label, value, trend, tone, icon: Icon }: MetricCardProps) {
  return (
    <article className={`card card-pad metric tone-${tone}`}>
      <div className="metric-head">
        <span className="metric-label">{label}</span>
        <span className="metric-icon">
          <Icon size={18} />
        </span>
      </div>

      <strong className="metric-value">{formatCurrency(value)}</strong>

      {trend && (
        <span
          className={trend === "income" ? "metric-trend income" : "metric-trend expense"}
        >
          {trend === "income" ? <ArrowUpRight size={15} /> : <ArrowDownRight size={15} />}{" "}
          {trend === "income" ? "Incoming" : "Outgoing"}
        </span>
      )}
    </article>
  );
}

// --- Date range filter -------------------------------------------------------

type RangeKey = "today" | "week" | "month" | "year" | "custom" | "all";

const RANGE_LABELS: Record<RangeKey, string> = {
  today: "Today",
  week: "This week",
  month: "This month",
  year: "This year",
  custom: "Custom range",
  all: "All time",
};

function toISODate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function rangeDates(key: RangeKey, today: Date): { from: Date; to: Date } {
  const year = today.getFullYear();
  const month = today.getMonth();

  switch (key) {
    case "today":
      return { from: today, to: today };
    case "week":
      return { from: new Date(year, month, today.getDate() - 6), to: today };
    case "month":
      return { from: new Date(year, month, 1), to: new Date(year, month + 1, 0) };
    case "year":
      return { from: new Date(year, 0, 1), to: new Date(year, 11, 31) };
    default:
      return { from: today, to: today };
  }
}

function DashboardSkeleton() {
  return (
    <>
      <PageHeader title="Dashboard" description={DESCRIPTION} />
      <div className="metric-grid">
        {Array.from({ length: 4 }, (_, index) => (
          <div className="skeleton metric" key={index} />
        ))}
      </div>
    </>
  );
}

export function DashboardPage() {
  const [range, setRange] = useState<RangeKey>("all");
  const [customFrom, setCustomFrom] = useState("");
  const [customTo, setCustomTo] = useState("");

  const { from, to, rangeError } = useMemo(() => {
    if (range === "all") {
      return { from: undefined, to: undefined, rangeError: null };
    }

    if (range === "custom") {
      if (!customFrom || !customTo) {
        return { from: undefined, to: undefined, rangeError: "Select a start and end date." };
      }
      if (customFrom > customTo) {
        return { from: undefined, to: undefined, rangeError: "Start date must be before end date." };
      }
      return { from: customFrom, to: customTo, rangeError: null };
    }

    const { from: start, to: end } = rangeDates(range, new Date());
    return { from: toISODate(start), to: toISODate(end), rangeError: null };
  }, [customFrom, customTo, range]);

  const dashboard = useQuery({
    queryKey: ["dashboard", from ?? "all", to ?? "all"],
    queryFn: () => financeApi.dashboard(from, to),
    enabled: !rangeError,
  });

  if (dashboard.isLoading || !dashboard.data) {
    return <DashboardSkeleton />;
  }

  if (dashboard.isError) {
    return (
      <>
        <PageHeader title="Dashboard" description={DESCRIPTION} />
        <ErrorState retry={() => dashboard.refetch()} />
      </>
    );
  }

  const data = dashboard.data;
  const totals = [
    { name: "Income", value: data.totalIncome },
    { name: "Expense", value: data.totalExpense },
  ];
  const hasActivity = data.totalIncome > 0 || data.totalExpense > 0;

  return (
    <>
      <PageHeader
        title="Dashboard"
        description={DESCRIPTION}
        actions={
          <RangeFilter
            range={range}
            onRangeChange={setRange}
            customFrom={customFrom}
            customTo={customTo}
            onCustomFromChange={setCustomFrom}
            onCustomToChange={setCustomTo}
          />
        }
        actionRow={rangeError ? <p className="field-error">{rangeError}</p> : undefined}
      />

      <section className="metric-grid">
        <MetricCard label="Current balance" value={data.totalBalance} tone="primary" icon={WalletCards} />
        <MetricCard label="Income" value={data.totalIncome} trend="income" tone="income" icon={ArrowUpRight} />
        <MetricCard label="Expenses" value={data.totalExpense} trend="expense" tone="expense" icon={ArrowDownRight} />
        <MetricCard label="Net balance" value={data.totalIncome - data.totalExpense} tone="info" icon={CreditCard} />
      </section>

      <section className="dashboard-grid section-offset">
        <article className="card card-pad">
          <h2>Expense by category</h2>
          <p className="subtle">Where your money is going.</p>
          <div className="chart-area">
            {data.expenseByCategory.length ? (
              <ResponsiveContainer>
                <BarChart data={data.expenseByCategory}>
                  <CartesianGrid stroke="var(--border)" vertical={false} />
                  <XAxis dataKey="categoryName" tick={{ fill: "var(--muted)", fontSize: 12 }} />
                  <YAxis tick={{ fill: "var(--muted)", fontSize: 12 }} />
                  <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                  <Legend />
                  <Bar
                    dataKey="totalAmount"
                    name="Amount"
                    fill={CHART_COLORS.expense}
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <EmptyState
                title="No expenses in this period"
                description="Add transactions or widen the date range to see the breakdown."
              />
            )}
          </div>
        </article>

        <article className="card card-pad">
          <h2>Income vs expense</h2>
          <p className="subtle">Current financial mix.</p>
          <div className="chart-area">
            {hasActivity ? (
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={totals} dataKey="value" nameKey="name" outerRadius={90}>
                    {totals.map((entry) => (
                      <Cell
                        key={entry.name}
                        fill={entry.name === "Income" ? CHART_COLORS.income : CHART_COLORS.expense}
                      />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <EmptyState
                title="No activity in this period"
                description="Income and expense totals will appear here."
              />
            )}
          </div>
        </article>
      </section>

      <section className="card section-offset">
        <div className="card-pad section-heading">
          <div>
            <h2>Recent transactions</h2>
            <p className="subtle">Your latest activity.</p>
          </div>
          <Link className="text-link" to="/transactions">
            View all
          </Link>
        </div>

        {data.recentTransactions.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Description</th>
                  <th>Category</th>
                  <th>Amount</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {data.recentTransactions.map((transaction) => (
                  <tr key={transaction.id}>
                    <td>{transaction.description || "Untitled transaction"}</td>
                    <td>
                      <span className="badge">{transaction.categoryName}</span>
                    </td>
                    <td>
                      <span
                        className={`amount ${
                          transaction.transactionType === "INCOME" ? "income" : "expense"
                        }`}
                      >
                        {formatCurrency(transaction.amount)}
                      </span>
                    </td>
                    <td>{formatDate(transaction.transactionDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No transactions in this period"
            description="Add transactions or widen the date range to see recent activity."
          />
        )}
      </section>

      <section className="quick-actions section-offset">
        <Link className="quick-action card" to="/accounts/new">
          <WalletCards size={20} />
          <span>
            <strong>Add an account</strong>
            <small>Track a new balance</small>
          </span>
        </Link>

        <Link className="quick-action card" to="/categories/new">
          <Plus size={20} />
          <span>
            <strong>Create a category</strong>
            <small>Keep spending organised</small>
          </span>
        </Link>

        <Link className="quick-action card" to="/rules/new">
          <ArrowUpRight size={20} />
          <span>
            <strong>Set a rule</strong>
            <small>Automate categorisation</small>
          </span>
        </Link>
      </section>
    </>
  );
}

interface RangeFilterProps {
  range: RangeKey;
  onRangeChange: (range: RangeKey) => void;
  customFrom: string;
  customTo: string;
  onCustomFromChange: (value: string) => void;
  onCustomToChange: (value: string) => void;
}

function RangeFilter({
  range,
  onRangeChange,
  customFrom,
  customTo,
  onCustomFromChange,
  onCustomToChange,
}: RangeFilterProps) {
  return (
    <div className="filter-group">
      <label className="filter-label">
        Period
        <select
          className="input compact-input"
          aria-label="Date range"
          value={range}
          onChange={(event) => onRangeChange(event.target.value as RangeKey)}
        >
          {(Object.keys(RANGE_LABELS) as RangeKey[]).map((key) => (
            <option key={key} value={key}>
              {RANGE_LABELS[key]}
            </option>
          ))}
        </select>
      </label>

      {range === "custom" && (
        <>
          <label className="filter-label">
            From
            <input
              type="date"
              className="input compact-input"
              value={customFrom}
              onChange={(event) => onCustomFromChange(event.target.value)}
            />
          </label>
          <label className="filter-label">
            To
            <input
              type="date"
              className="input compact-input"
              value={customTo}
              onChange={(event) => onCustomToChange(event.target.value)}
            />
          </label>
        </>
      )}
    </div>
  );
}
