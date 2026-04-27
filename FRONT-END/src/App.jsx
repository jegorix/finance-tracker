import { useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, Route, Routes } from 'react-router-dom';
import {
  createAccount,
  createBudget,
  createCategory,
  createTransaction,
  createUser,
  deleteAccount,
  deleteBudget,
  deleteCategory,
  deleteTransaction,
  deleteUser,
  listAccounts,
  listBudgets,
  listCategories,
  listUsers,
  searchTransactions,
  updateAccount,
  updateBudget,
  updateCategory,
  updateTransaction,
  updateUser
} from './api';

const ACCOUNT_TYPES = ['CHECKING', 'SAVINGS', 'CREDIT', 'DEBIT', 'INVESTMENT', 'CASH'];
const TRANSACTION_TYPES = ['INCOME', 'EXPENSE', 'TRANSFER'];
const USER_PAGE_SIZE = 5;

const INITIAL_TRANSACTION_FILTERS = {
  budgetName: '',
  accountName: '',
  minAmount: '',
  maxAmount: '',
  startDateTime: '',
  endDateTime: '',
  type: 'ALL',
  queryMode: 'JPQL',
  page: 0,
  size: 5,
  sortBy: 'occurredAt',
  ascending: false
};

const EMPTY_PAGE = {
  number: 0,
  size: 0,
  totalElements: 0,
  totalPages: 0
};

const DASHBOARD_TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'statistics', label: 'Statistics' },
  { id: 'profile', label: 'Profile' },
  { id: 'accounts', label: 'Accounts' },
  { id: 'planning', label: 'Planning' },
  { id: 'transactions', label: 'Transactions' }
];

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/app" element={<DashboardPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function LandingPage() {
  return (
    <div className="landing-page">
      <header className="landing-header">
        <div className="landing-header-inner">
          <div>
            <h1>Finance Tracker</h1>
            <p className="landing-subtitle">
              Keep accounts, budgets and everyday spending in one clear workspace.
            </p>
          </div>
          <div className="header-auth-actions">
            <Link className="btn btn-soft" to="/app">Open app</Link>
          </div>
        </div>
      </header>

      <main className="landing-main">
        <section className="hero-card">
          <div className="hero-copy">
            <span className="eyebrow">Personal Finance</span>
            <h2>Track money, plans and activity in one place.</h2>
            <p>
              Stay on top of balances, plan your spending, organise categories
              and review recent transactions without jumping between screens.
            </p>
            <div className="hero-actions">
              <Link className="btn btn-primary" to="/app">Go to dashboard</Link>
            </div>
          </div>

          <div className="hero-metrics">
            <MetricCard label="Profiles" value="People and shared finances" />
            <MetricCard label="Accounts" value="Cards, cash and savings" />
            <MetricCard label="Budgets" value="Plans for every goal" />
            <MetricCard label="Activity" value="Recent income and spending" />
          </div>
        </section>
      </main>

      <section className="landing-footer reveal">
        <div className="footer-brand-area">
          <h2>Finance Tracker</h2>
          <p>
            A clean workspace for managing money, budgets and daily transactions.
          </p>
          <small>Accounts, budgets, categories and transactions</small>
        </div>

        <div className="footer-col">
          <h4>Entities</h4>
          <span>Profiles</span>
          <span>Accounts</span>
          <span>Budgets</span>
          <span>Categories</span>
          <span>Transactions</span>
        </div>

        <div className="footer-col">
          <h4>Highlights</h4>
          <span>Fast profile switching</span>
          <span>Clear budget planning</span>
          <span>Flexible transaction filters</span>
          <span>Focused daily workflow</span>
        </div>
      </section>
    </div>
  );
}

function DashboardPage() {
  const bootRef = useRef(false);

  const [loading, setLoading] = useState(true);
  const [transactionLoading, setTransactionLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [users, setUsers] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [budgets, setBudgets] = useState([]);
  const [categories, setCategories] = useState([]);
  const [transactions, setTransactions] = useState([]);

  const [transactionPage, setTransactionPage] = useState(EMPTY_PAGE);

  const [selectedUserId, setSelectedUserId] = useState(null);
  const [modalState, setModalState] = useState({ type: null, mode: 'create', payload: null });
  const [pendingDelete, setPendingDelete] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [userPage, setUserPage] = useState(0);

  const [userSearch, setUserSearch] = useState('');
  const [accountSearch, setAccountSearch] = useState('');
  const [accountTypeFilter, setAccountTypeFilter] = useState('ALL');
  const [budgetSearch, setBudgetSearch] = useState('');
  const [budgetCategoryFilter, setBudgetCategoryFilter] = useState('ALL');
  const [categorySearch, setCategorySearch] = useState('');
  const [transactionFilters, setTransactionFilters] = useState(INITIAL_TRANSACTION_FILTERS);

  const deferredUserSearch = useDeferredValue(userSearch);
  const deferredAccountSearch = useDeferredValue(accountSearch);
  const deferredBudgetSearch = useDeferredValue(budgetSearch);
  const deferredCategorySearch = useDeferredValue(categorySearch);

  useEffect(() => {
    if (bootRef.current) {
      return;
    }

    bootRef.current = true;
    void refreshAll(INITIAL_TRANSACTION_FILTERS);
  }, []);

  useEffect(() => {
    if (!notice) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => setNotice(''), 3200);
    return () => window.clearTimeout(timeoutId);
  }, [notice]);

  useEffect(() => {
    if (!error) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => setError(''), 5000);
    return () => window.clearTimeout(timeoutId);
  }, [error]);

  useEffect(() => {
    if (!users.length) {
      setSelectedUserId(null);
      return;
    }

    if (!selectedUserId || !users.some((user) => user.id === selectedUserId)) {
      setSelectedUserId(users[0].id);
    }
  }, [users, selectedUserId]);

  const selectedUser = useMemo(
    () => users.find((item) => item.id === selectedUserId) || null,
    [users, selectedUserId]
  );

  const accountMap = useMemo(
    () => new Map(accounts.map((item) => [item.id, item])),
    [accounts]
  );

  const budgetMap = useMemo(
    () => new Map(budgets.map((item) => [item.id, item])),
    [budgets]
  );

  const categoryMap = useMemo(
    () => new Map(categories.map((item) => [item.id, item])),
    [categories]
  );

  const selectedUserAccountIds = useMemo(
    () => new Set(selectedUser?.accountIds || []),
    [selectedUser]
  );

  const selectedUserBudgetIds = useMemo(
    () => new Set(selectedUser?.budgetIds || []),
    [selectedUser]
  );

  const filteredUsers = useMemo(() => {
    const query = deferredUserSearch.trim().toLowerCase();
    if (!query) {
      return users;
    }

    return users.filter(
      (item) =>
        item.username.toLowerCase().includes(query) ||
        item.email.toLowerCase().includes(query)
    );
  }, [users, deferredUserSearch]);

  const visibleUsers = useMemo(
    () => filteredUsers.slice(userPage * USER_PAGE_SIZE, (userPage + 1) * USER_PAGE_SIZE),
    [filteredUsers, userPage]
  );
  const totalUserPages = Math.max(1, Math.ceil(filteredUsers.length / USER_PAGE_SIZE));

  useEffect(() => {
    setUserPage(0);
  }, [deferredUserSearch]);

  useEffect(() => {
    if (userPage >= totalUserPages) {
      setUserPage(Math.max(0, totalUserPages - 1));
    }
  }, [userPage, totalUserPages]);

  const visibleAccounts = useMemo(() => {
    return accounts
      .filter((item) => (selectedUser ? selectedUserAccountIds.has(item.id) : true))
      .filter((item) =>
        accountTypeFilter === 'ALL' ? true : item.type === accountTypeFilter
      )
      .filter((item) => matchesText(item.name, deferredAccountSearch));
  }, [accounts, selectedUser, selectedUserAccountIds, accountTypeFilter, deferredAccountSearch]);

  const visibleBudgets = useMemo(() => {
    return budgets
      .filter((item) => (selectedUser ? item.userId === selectedUser.id : true))
      .filter((item) => matchesText(item.name, deferredBudgetSearch))
      .filter((item) =>
        budgetCategoryFilter === 'ALL'
          ? true
          : item.categoryIds?.includes(Number(budgetCategoryFilter))
      );
  }, [budgets, selectedUser, deferredBudgetSearch, budgetCategoryFilter]);

  const visibleCategories = useMemo(() => {
    return categories
      .filter((item) => (selectedUser ? item.userId === selectedUser.id : true))
      .filter((item) => matchesText(item.name, deferredCategorySearch));
  }, [categories, selectedUser, deferredCategorySearch]);

  const visibleTransactions = useMemo(() => {
    return transactions
      .filter((item) => (selectedUser ? selectedUserAccountIds.has(item.accountId) : true))
      .filter((item) => (
        transactionFilters.type === 'ALL' ? true : item.type === transactionFilters.type
      ));
  }, [transactions, transactionFilters.type, selectedUser, selectedUserAccountIds]);

  const relationshipAccounts = useMemo(
    () => (selectedUser?.accountIds || []).map((id) => accountMap.get(id)).filter(Boolean),
    [selectedUser, accountMap]
  );

  const relationshipBudgets = useMemo(
    () => (selectedUser?.budgetIds || []).map((id) => budgetMap.get(id)).filter(Boolean),
    [selectedUser, budgetMap]
  );

  const relatedTransactionCount = useMemo(() => visibleTransactions.length, [visibleTransactions]);
  const totalBalance = useMemo(
    () => visibleAccounts.reduce((sum, account) => sum + Number(account.balance || 0), 0),
    [visibleAccounts]
  );
  const totalBudgetLimit = useMemo(
    () => visibleBudgets.reduce((sum, budget) => sum + Number(budget.limitAmount || 0), 0),
    [visibleBudgets]
  );
  const netCashflow = useMemo(
    () => visibleTransactions.reduce((sum, transaction) => {
      const amount = Number(transaction.amount || 0);
      if (transaction.type === 'INCOME') {
        return sum + amount;
      }
      if (transaction.type === 'EXPENSE') {
        return sum - amount;
      }
      return sum;
    }, 0),
    [visibleTransactions]
  );
  const cashflowChart = useMemo(() => {
    const income = visibleTransactions
      .filter((transaction) => transaction.type === 'INCOME')
      .reduce((sum, transaction) => sum + Number(transaction.amount || 0), 0);
    const expense = visibleTransactions
      .filter((transaction) => transaction.type === 'EXPENSE')
      .reduce((sum, transaction) => sum + Number(transaction.amount || 0), 0);
    const transfer = visibleTransactions
      .filter((transaction) => transaction.type === 'TRANSFER')
      .reduce((sum, transaction) => sum + Number(transaction.amount || 0), 0);
    const groupedByDay = visibleTransactions.reduce((accumulator, transaction) => {
      const key = (transaction.date || transaction.occurredAt || '').slice(0, 10);
      if (!key) {
        return accumulator;
      }

      const current = accumulator.get(key) || { date: key, income: 0, expense: 0, transfer: 0 };
      const amount = Number(transaction.amount || 0);

      if (transaction.type === 'INCOME') {
        current.income += amount;
      }

      if (transaction.type === 'EXPENSE') {
        current.expense += amount;
      }

      if (transaction.type === 'TRANSFER') {
        current.transfer += amount;
      }

      accumulator.set(key, current);
      return accumulator;
    }, new Map());
    const points = Array.from(groupedByDay.values())
      .sort((left, right) => left.date.localeCompare(right.date))
      .slice(-7);
    const chartMax = points.reduce(
      (maxValue, point) => Math.max(maxValue, point.income, point.expense, point.transfer),
      1
    );

    return {
      income,
      expense,
      transfer,
      points,
      maxValue: chartMax
    };
  }, [visibleTransactions]);
  const statisticsSummary = useMemo(() => {
    const topIncome = [...visibleTransactions]
      .filter((transaction) => transaction.type === 'INCOME')
      .sort((left, right) => Number(right.amount || 0) - Number(left.amount || 0))[0] || null;
    const topExpense = [...visibleTransactions]
      .filter((transaction) => transaction.type === 'EXPENSE')
      .sort((left, right) => Number(right.amount || 0) - Number(left.amount || 0))[0] || null;
    const averageTransaction = visibleTransactions.length
      ? visibleTransactions.reduce((sum, transaction) => sum + Number(transaction.amount || 0), 0) / visibleTransactions.length
      : 0;

    return {
      topIncome,
      topExpense,
      averageTransaction
    };
  }, [visibleTransactions]);
  const isDisconnected = Boolean(error) && !loading && users.length === 0 && accounts.length === 0 && budgets.length === 0;

  async function refreshAll(nextTransactionFilters = transactionFilters, preferredUserId = selectedUserId) {
    setLoading(true);
    setError('');

    try {
      const [usersData, accountsData, budgetsData, categoriesData] = await Promise.all([
        listUsers(),
        listAccounts(),
        listBudgets(),
        listCategories()
      ]);
      const enrichedUsers = usersData.map((user) => ({
        ...user,
        accountIds: accountsData
          .filter((account) => account.userId === user.id)
          .map((account) => account.id),
        budgetIds: budgetsData
          .filter((budget) => budget.userId === user.id)
          .map((budget) => budget.id)
      }));
      const targetUserId = preferredUserId && enrichedUsers.some((user) => user.id === preferredUserId)
        ? preferredUserId
        : enrichedUsers[0]?.id ?? null;

      setUsers(enrichedUsers);
      setAccounts(accountsData);
      setBudgets(budgetsData);
      setCategories(categoriesData);
      setSelectedUserId(targetUserId);

      await refreshTransactions(nextTransactionFilters, targetUserId);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function refreshTransactions(nextFilters = transactionFilters, userId = selectedUserId) {
    if (!userId) {
      setTransactions([]);
      setTransactionPage(EMPTY_PAGE);
      return;
    }

    setTransactionLoading(true);

    try {
      const payload = await searchTransactions({
        userId,
        budgetName: nextFilters.budgetName,
        accountName: nextFilters.accountName,
        minAmount: nextFilters.minAmount,
        maxAmount: nextFilters.maxAmount,
        startDateTime: normalizeDateTime(nextFilters.startDateTime),
        endDateTime: normalizeDateTime(nextFilters.endDateTime),
        queryMode: nextFilters.queryMode,
        page: nextFilters.page,
        size: nextFilters.size,
        sortBy: nextFilters.sortBy,
        ascending: nextFilters.ascending
      });

      setTransactions(payload.content);
      setTransactionPage(payload.page);
    } catch (err) {
      setError(err.message);
    } finally {
      setTransactionLoading(false);
    }
  }

  async function submitUser(values, mode, original) {
    let savedUser;
    if (mode === 'create') {
      savedUser = await createUser(values);
      setNotice(`Profile "${values.username}" created.`);
    } else {
      savedUser = await updateUser(original.id, values);
      setNotice(`Profile "${values.username}" updated.`);
    }

    await refreshAll(transactionFilters, savedUser.id);
    return savedUser;
  }

  async function submitAccount(values, mode, original) {
    if (mode === 'create') {
      await createAccount(values);
      setNotice(`Account "${values.name}" created.`);
    } else {
      await updateAccount(original.id, values);
      setNotice(`Account "${values.name}" updated.`);
    }

    await refreshAll();
  }

  async function submitBudget(values, mode, original) {
    if (mode === 'create') {
      await createBudget(values);
      setNotice(`Budget "${values.name}" created.`);
    } else {
      await updateBudget(original.id, values);
      setNotice(`Budget "${values.name}" updated.`);
    }

    await refreshAll();
  }

  async function submitCategory(values, mode, original) {
    if (mode === 'create') {
      await createCategory(values);
      setNotice(`Category "${values.name}" created.`);
    } else {
      await updateCategory(original.id, values);
      setNotice(`Category "${values.name}" updated.`);
    }

    await refreshAll();
  }

  async function submitTransaction(values, mode, original) {
    if (mode === 'create') {
      await createTransaction(values);
      setNotice(`Transaction "${values.description}" created.`);
    } else {
      await updateTransaction(original.id, values);
      setNotice(`Transaction "${values.description}" updated.`);
    }

    await refreshAll(transactionFilters);
  }

  async function handleDelete(kind, item) {
    try {
      if (kind === 'user') {
        await deleteUser(item.id);
        setNotice('Profile deleted.');
        await refreshAll(transactionFilters);
        return;
      }

      if (kind === 'account') {
        await deleteAccount(item.id);
      }

      if (kind === 'budget') {
        await deleteBudget(item.id);
      }

      if (kind === 'category') {
        await deleteCategory(item.id);
      }

      if (kind === 'transaction') {
        await deleteTransaction(item.id);
      }

      setNotice(`${capitalize(kind)} deleted.`);
      await refreshAll(transactionFilters);
    } catch (err) {
      setError(err.message);
    }
  }

  function requestDelete(kind, item) {
    setPendingDelete({ kind, item });
  }

  async function confirmDelete() {
    if (!pendingDelete) {
      return;
    }

    const { kind, item } = pendingDelete;
    setPendingDelete(null);
    await handleDelete(kind, item);
  }

  function openModal(type, mode, payload = null) {
    setModalState({ type, mode, payload });
  }

  function closeModal() {
    setModalState({ type: null, mode: 'create', payload: null });
  }

  function updateTransactionFilter(name, value) {
    setTransactionFilters((current) => ({
      ...current,
      [name]: value
    }));
  }

  function handleSelectUser(userId) {
    const nextFilters = {
      ...transactionFilters,
      page: 0
    };

    setSelectedUserId(userId);
    setTransactionFilters(nextFilters);
    void refreshTransactions(nextFilters, userId);
  }

  async function applyTransactionFilters() {
    const nextFilters = {
      ...transactionFilters,
      page: 0
    };

    setTransactionFilters(nextFilters);
    await refreshTransactions(nextFilters);
  }

  async function resetTransactionFilters() {
    setTransactionFilters(INITIAL_TRANSACTION_FILTERS);
    await refreshTransactions(INITIAL_TRANSACTION_FILTERS);
  }

  async function changeTransactionPage(delta) {
    const nextPage = transactionFilters.page + delta;
    if (nextPage < 0) {
      return;
    }

    if (transactionPage.totalPages && nextPage >= transactionPage.totalPages) {
      return;
    }

    const nextFilters = {
      ...transactionFilters,
      page: nextPage
    };

    setTransactionFilters(nextFilters);
    await refreshTransactions(nextFilters);
  }

  const transactionModalContext = useMemo(
    () => ({
      users,
      accounts,
      budgets,
      selectedUserId
    }),
    [users, accounts, budgets, selectedUserId]
  );

  function resetDashboardState() {
    setUsers([]);
    setAccounts([]);
    setBudgets([]);
    setCategories([]);
    setTransactions([]);
    setTransactionPage(EMPTY_PAGE);
    setSelectedUserId(null);
    setModalState({ type: null, mode: 'create', payload: null });
    setPendingDelete(null);
  }

  return (
    <div className="dashboard-layout">
      <header className="app-topbar">
        <div className="app-topbar-brand">
          <div className="brand-mark">FT</div>
          <div>
            <strong>Finance Tracker</strong>
            <small>Your personal finance workspace</small>
          </div>
        </div>

        <div className="app-topbar-actions">
          <Link className="btn btn-topbar-ghost" to="/">Home</Link>
        </div>
      </header>

      <main className="board-shell">
        <section className="dashboard-main">
          <section className="summary-hero card summary-hero-expanded">
            <div className="summary-hero-main">
              <div className="summary-profile">
                <div className="summary-avatar">
                  {selectedUser ? selectedUser.username.slice(0, 2).toUpperCase() : 'FT'}
                </div>
                <div>
                  <span className="eyebrow">Current profile</span>
                  <h3>{selectedUser ? selectedUser.username : 'Choose a profile'}</h3>
                  <p className="muted">
                    {selectedUser ? selectedUser.email : 'Select a profile to manage accounts, plans and activity.'}
                  </p>
                </div>
              </div>

              <div className="summary-actions">
                <button
                  className="btn btn-soft"
                  type="button"
                  onClick={() => openModal('user', selectedUser ? 'edit' : 'create', selectedUser)}
                >
                  {selectedUser ? 'Edit profile' : 'Create profile'}
                </button>
                <button className="btn btn-ghost" type="button" onClick={() => void refreshAll(transactionFilters)}>
                  Refresh data
                </button>
              </div>
            </div>

            <div className="summary-stats">
              <MetricCard label="Total balance" value={formatCurrency(totalBalance)} />
              <MetricCard label="Budget plans" value={formatCurrency(totalBudgetLimit)} />
              <MetricCard label="Net flow" value={formatCurrency(netCashflow)} />
              <MetricCard label="Accounts" value={relationshipAccounts.length} />
              <MetricCard label="Transactions" value={relatedTransactionCount} />
            </div>
          </section>

          {isDisconnected && (
            <section className="connection-banner card">
              <div>
                <h3>Unable to load data</h3>
                <p className="muted">
                  Start the backend server on http://localhost:8080, then use refresh.
                </p>
              </div>
              <button className="btn btn-primary" type="button" onClick={() => void refreshAll(transactionFilters)}>
                Try again
              </button>
            </section>
          )}

          <nav className="tab-bar" aria-label="Dashboard sections">
            {DASHBOARD_TABS.map((tab) => (
              <button
                key={tab.id}
                className={activeTab === tab.id ? 'tab-btn active' : 'tab-btn'}
                type="button"
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </nav>

          <div className="tab-stage">
            {activeTab === 'overview' && (
              <div className="single-column-stage profile-stage">
                <PanelSection
                  title="Profile snapshot"
                  subtitle="A quick summary of the selected profile"
                >
                  {selectedUser ? (
                    <div className="stack">
                      <div className="relationship-hero">
                        <div>
                          <strong>{selectedUser.username}</strong>
                          <p>{selectedUser.email}</p>
                        </div>
                        <span className="chip active">Current profile</span>
                      </div>

                      <div className="stats-row three">
                        <MetricPill label="Accounts" value={relationshipAccounts.length} />
                        <MetricPill label="Budgets" value={relationshipBudgets.length} />
                        <MetricPill label="Transactions" value={relatedTransactionCount} />
                      </div>

                      <div className="relationship-block">
                        <h4>Accounts</h4>
                        <div className="chip-cloud">
                          {relationshipAccounts.map((account) => (
                            <span className="chip" key={account.id}>
                              {account.name} · {formatEnumLabel(account.type)}
                            </span>
                          ))}
                          {!relationshipAccounts.length && <EmptyState text="No accounts yet." compact />}
                        </div>
                      </div>

                      <div className="relationship-block">
                        <h4>Budgets</h4>
                        <div className="chip-cloud">
                          {relationshipBudgets.map((budget) => (
                            <span className="chip" key={budget.id}>
                              {budget.name} · {formatCurrency(budget.limitAmount)}
                            </span>
                          ))}
                          {!relationshipBudgets.length && <EmptyState text="No budgets yet." compact />}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <EmptyState text="Create or select a profile to see a summary here." />
                  )}
                </PanelSection>
                <PanelSection
                  title="Workspace actions"
                  subtitle="Primary actions for the selected profile"
                >
                  <div className="quick-actions-grid">
                    <button className="quick-action-card" type="button" onClick={() => openModal('account', 'create')}>
                      <strong>Add account</strong>
                      <span>Cards, cash or savings</span>
                    </button>
                    <button className="quick-action-card" type="button" onClick={() => openModal('budget', 'create')}>
                      <strong>Add budget</strong>
                      <span>Monthly plans and goals</span>
                    </button>
                    <button className="quick-action-card" type="button" onClick={() => openModal('transaction', 'create')}>
                      <strong>Add transaction</strong>
                      <span>Record income, expenses and transfers</span>
                    </button>
                    <button className="quick-action-card" type="button" onClick={() => openModal('category', 'create')}>
                      <strong>Add category</strong>
                      <span>Food, travel, home and more</span>
                    </button>
                    <button className="quick-action-card" type="button" onClick={() => setActiveTab('transactions')}>
                      <strong>Review activity</strong>
                      <span>Open transaction history</span>
                    </button>
                  </div>
                </PanelSection>
              </div>
            )}

            {activeTab === 'statistics' && (
              <div className="single-column-stage">
                <PanelSection
                  title="Statistics"
                  subtitle="Cashflow, transfers and standout transactions for the selected profile"
                >
                  {visibleTransactions.length ? (
                    <CashflowChart
                      income={cashflowChart.income}
                      expense={cashflowChart.expense}
                      transfer={cashflowChart.transfer}
                      points={cashflowChart.points}
                      maxValue={cashflowChart.maxValue}
                    />
                  ) : (
                    <EmptyState text="No transactions yet for the selected profile." />
                  )}
                </PanelSection>

                <div className="planning-grid">
                  <PanelSection
                    title="Cashflow stats"
                    subtitle="Useful aggregate metrics for the current activity set"
                  >
                    <div className="stats-row three">
                      <MetricPill label="Income" value={formatCurrency(cashflowChart.income)} />
                      <MetricPill label="Expense" value={formatCurrency(cashflowChart.expense)} />
                      <MetricPill label="Transfer" value={formatCurrency(cashflowChart.transfer)} />
                    </div>
                    <div className="stats-row three">
                      <MetricPill label="Net" value={formatCurrency(netCashflow)} />
                      <MetricPill label="Avg transaction" value={formatCurrency(statisticsSummary.averageTransaction)} />
                      <MetricPill label="Active days" value={cashflowChart.points.length} />
                    </div>
                  </PanelSection>

                  <PanelSection
                    title="Largest movements"
                    subtitle="Biggest incoming and outgoing operations in the current selection"
                  >
                    <div className="entity-list">
                      <article className="entity-card entity-card-prominent">
                        <div className="entity-head">
                          <div>
                            <h4>Largest income</h4>
                            <p>{statisticsSummary.topIncome?.description || 'No income transactions yet'}</p>
                          </div>
                          <span className="chip active">
                            {statisticsSummary.topIncome ? formatCurrency(statisticsSummary.topIncome.amount) : '—'}
                          </span>
                        </div>
                        <div className="meta-grid">
                          <span>Date: {statisticsSummary.topIncome ? formatDateTime(statisticsSummary.topIncome.occurredAt) : '—'}</span>
                          <span>Account: {statisticsSummary.topIncome?.accountName || '—'}</span>
                        </div>
                      </article>

                      <article className="entity-card entity-card-prominent">
                        <div className="entity-head">
                          <div>
                            <h4>Largest expense</h4>
                            <p>{statisticsSummary.topExpense?.description || 'No expense transactions yet'}</p>
                          </div>
                          <span className="chip">
                            {statisticsSummary.topExpense ? formatCurrency(statisticsSummary.topExpense.amount) : '—'}
                          </span>
                        </div>
                        <div className="meta-grid">
                          <span>Date: {statisticsSummary.topExpense ? formatDateTime(statisticsSummary.topExpense.occurredAt) : '—'}</span>
                          <span>Account: {statisticsSummary.topExpense?.accountName || '—'}</span>
                        </div>
                      </article>
                    </div>
                  </PanelSection>
                </div>
              </div>
            )}

            {activeTab === 'profile' && (
              <div className="single-column-stage">
                <PanelSection
                  title="Profile"
                  subtitle="All profiles in the system"
                  actionLabel="Add profile"
                  onAction={() => openModal('user', 'create')}
                >
                  <div className="stack">
                    <div className="profile-toolbar">
                      <input
                        value={userSearch}
                        onChange={(event) => setUserSearch(event.target.value)}
                        placeholder="Search profile by name or email"
                      />
                      <span className="muted profile-page-indicator">
                        Page {totalUserPages ? userPage + 1 : 1} of {totalUserPages}
                      </span>
                    </div>

                    <div className="users-list">
                      {visibleUsers.map((user) => (
                        <article
                          key={user.id}
                          className={selectedUserId === user.id ? 'user-card active-card' : 'user-card clickable'}
                          onClick={() => handleSelectUser(user.id)}
                        >
                          <header>
                            <div className="user-head">
                              <div className="avatar-badge">{user.username.slice(0, 2).toUpperCase()}</div>
                              <div>
                                <strong>{user.username}</strong>
                                <p>{user.email}</p>
                              </div>
                            </div>

                            <div className="row-actions">
                              <button
                                className="icon-btn"
                                type="button"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  openModal('user', 'edit', user);
                                }}
                              >
                                Edit
                              </button>
                              <button
                                className="icon-btn danger"
                                type="button"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  requestDelete('user', user);
                                }}
                              >
                                Delete
                              </button>
                            </div>
                          </header>

                          <div className="stats-row">
                            <MetricPill label="Accounts" value={user.accountIds?.length || 0} />
                            <MetricPill label="Budgets" value={user.budgetIds?.length || 0} />
                          </div>
                        </article>
                      ))}

                      {!visibleUsers.length && <EmptyState text="No profiles match your search." />}
                    </div>

                    {filteredUsers.length > USER_PAGE_SIZE && (
                      <div className="pagination-row user-pagination">
                        <button
                          className="btn btn-ghost"
                          disabled={userPage <= 0}
                          type="button"
                          onClick={() => setUserPage((current) => Math.max(0, current - 1))}
                        >
                          Previous
                        </button>
                        <button
                          className="btn btn-ghost"
                          disabled={userPage >= totalUserPages - 1}
                          type="button"
                          onClick={() => setUserPage((current) => Math.min(totalUserPages - 1, current + 1))}
                        >
                          Next
                        </button>
                      </div>
                    )}
                  </div>
                </PanelSection>
              </div>
            )}

            {activeTab === 'accounts' && (
              <PanelSection
                title="Accounts"
                subtitle="Accounts for the selected profile"
                actionLabel="Add account"
                onAction={() => openModal('account', 'create')}
              >
                <div className="filter-row">
                  <input
                    value={accountSearch}
                    onChange={(event) => setAccountSearch(event.target.value)}
                    placeholder="Search account name"
                  />
                  <select
                    value={accountTypeFilter}
                    onChange={(event) => setAccountTypeFilter(event.target.value)}
                  >
                    <option value="ALL">All types</option>
                    {ACCOUNT_TYPES.map((type) => (
                      <option key={type} value={type}>{formatEnumLabel(type)}</option>
                    ))}
                  </select>
                </div>

                <div className="entity-grid">
                  {visibleAccounts.map((account) => (
                    <article className="entity-card entity-card-prominent" key={account.id}>
                      <div className="entity-head">
                        <div>
                          <h4>{account.name}</h4>
                          <p>{formatEnumLabel(account.type)}</p>
                        </div>
                        <div className="row-actions">
                          <button className="icon-btn" type="button" onClick={() => openModal('account', 'edit', account)}>
                            Edit
                          </button>
                          <button className="icon-btn danger" type="button" onClick={() => requestDelete('account', account)}>
                            Delete
                          </button>
                        </div>
                      </div>
                      <div className="meta-grid">
                        <span>Balance: {formatCurrency(account.balance)}</span>
                        <span>Profile: {selectedUser?.username || 'Not selected'}</span>
                      </div>
                    </article>
                  ))}

                  {!visibleAccounts.length && <EmptyState text="No accounts yet for this profile." />}
                </div>
              </PanelSection>
            )}

            {activeTab === 'planning' && (
              <div className="planning-grid">
                <PanelSection
                  title="Budgets"
                  subtitle="Set spending limits and connect them to categories"
                  actionLabel="Add budget"
                  onAction={() => openModal('budget', 'create')}
                >
                  <div className="filter-row">
                    <input
                      value={budgetSearch}
                      onChange={(event) => setBudgetSearch(event.target.value)}
                      placeholder="Search budget name"
                    />
                    <select
                      value={budgetCategoryFilter}
                      onChange={(event) => setBudgetCategoryFilter(event.target.value)}
                    >
                      <option value="ALL">All categories</option>
                      {visibleCategories.map((category) => (
                        <option key={category.id} value={category.id}>{category.name}</option>
                      ))}
                    </select>
                  </div>

                  <div className="entity-list">
                    {visibleBudgets.map((budget) => (
                      <article className="entity-card" key={budget.id}>
                        <div className="entity-head">
                          <div>
                            <h4>{budget.name}</h4>
                            <p>{formatDate(budget.periodStart)} {'->'} {formatDate(budget.periodEnd)}</p>
                          </div>
                          <div className="row-actions">
                            <button className="icon-btn" type="button" onClick={() => openModal('budget', 'edit', budget)}>
                              Edit
                            </button>
                            <button className="icon-btn danger" type="button" onClick={() => requestDelete('budget', budget)}>
                              Delete
                            </button>
                          </div>
                        </div>

                        <div className="meta-grid">
                          <span>Limit: {formatCurrency(budget.limitAmount)}</span>
                          <span>Transactions: {budget.transactionIds?.length || 0}</span>
                        </div>

                        <div className="chip-cloud">
                          {(budget.categoryIds || []).map((id) => (
                            <span className="chip" key={id}>
                              {categoryMap.get(id)?.name || 'Category unavailable'}
                            </span>
                          ))}
                          {(!budget.categoryIds || budget.categoryIds.length === 0) && (
                            <span className="chip muted-chip">No categories added</span>
                          )}
                        </div>
                      </article>
                    ))}

                    {!visibleBudgets.length && <EmptyState text="No budgets yet for this profile." />}
                  </div>
                </PanelSection>

                <PanelSection
                  title="Categories"
                  subtitle="Organise spending areas for the selected profile"
                  actionLabel="Add category"
                  onAction={() => openModal('category', 'create')}
                >
                  <div className="filter-row single">
                    <input
                      value={categorySearch}
                      onChange={(event) => setCategorySearch(event.target.value)}
                      placeholder="Search category name"
                    />
                  </div>

                  <div className="entity-list">
                    {visibleCategories.map((category) => (
                      <article className="entity-card" key={category.id}>
                        <div className="entity-head">
                          <div>
                            <h4>{category.name}</h4>
                            <p>{selectedUser?.username || 'Profile not selected'}</p>
                          </div>
                          <div className="row-actions">
                            <button className="icon-btn" type="button" onClick={() => openModal('category', 'edit', category)}>
                              Edit
                            </button>
                            <button className="icon-btn danger" type="button" onClick={() => requestDelete('category', category)}>
                              Delete
                            </button>
                          </div>
                        </div>

                        <div className="chip-cloud">
                          {(category.budgetIds || []).map((id) => (
                            <span className="chip" key={id}>
                              {budgetMap.get(id)?.name || 'Budget unavailable'}
                            </span>
                          ))}
                          {(!category.budgetIds || category.budgetIds.length === 0) && (
                            <span className="chip muted-chip">Not used in budgets yet</span>
                          )}
                        </div>
                      </article>
                    ))}

                    {!visibleCategories.length && <EmptyState text="No categories yet for this profile." />}
                  </div>
                </PanelSection>
              </div>
            )}

            {activeTab === 'transactions' && (
              <PanelSection
                title="Transactions"
                subtitle="Find recent activity by account, budget, amount or date"
                actionLabel="Add transaction"
                onAction={() => openModal('transaction', 'create')}
              >
                <div className="stack">
                  <div className="filter-grid">
                    <label className="field compact">
                      <span>Budget name</span>
                      <input
                        value={transactionFilters.budgetName}
                        onChange={(event) => updateTransactionFilter('budgetName', event.target.value)}
                        placeholder="Food"
                      />
                    </label>

                    <label className="field compact">
                      <span>Account name</span>
                      <input
                        value={transactionFilters.accountName}
                        onChange={(event) => updateTransactionFilter('accountName', event.target.value)}
                        placeholder="Main card"
                      />
                    </label>

                    <label className="field compact">
                      <span>Min amount</span>
                      <input
                        type="number"
                        step="0.01"
                        value={transactionFilters.minAmount}
                        onChange={(event) => updateTransactionFilter('minAmount', event.target.value)}
                      />
                    </label>

                    <label className="field compact">
                      <span>Max amount</span>
                      <input
                        type="number"
                        step="0.01"
                        value={transactionFilters.maxAmount}
                        onChange={(event) => updateTransactionFilter('maxAmount', event.target.value)}
                      />
                    </label>

                    <label className="field compact">
                      <span>Start date/time</span>
                      <input
                        type="datetime-local"
                        value={transactionFilters.startDateTime}
                        onChange={(event) => updateTransactionFilter('startDateTime', event.target.value)}
                      />
                    </label>

                    <label className="field compact">
                      <span>End date/time</span>
                      <input
                        type="datetime-local"
                        value={transactionFilters.endDateTime}
                        onChange={(event) => updateTransactionFilter('endDateTime', event.target.value)}
                      />
                    </label>

                    <label className="field compact">
                      <span>Type</span>
                      <select
                        value={transactionFilters.type}
                        onChange={(event) => updateTransactionFilter('type', event.target.value)}
                      >
                        <option value="ALL">All types</option>
                        {TRANSACTION_TYPES.map((type) => (
                          <option key={type} value={type}>{formatEnumLabel(type)}</option>
                        ))}
                      </select>
                    </label>
                  </div>

                  <div className="toolbar-actions">
                    <button className="btn btn-primary" type="button" onClick={() => void applyTransactionFilters()}>
                      Apply filters
                    </button>
                    <button className="btn btn-ghost" type="button" onClick={() => void resetTransactionFilters()}>
                      Reset
                    </button>
                  </div>

                  <div className="stats-row three">
                    <MetricPill label="Shown" value={visibleTransactions.length} />
                    <MetricPill label="Page" value={transactionPage.totalPages ? transactionPage.number + 1 : 1} />
                    <MetricPill label="Total" value={transactionPage.totalElements} />
                  </div>

                  <div className="entity-list transactions-list">
                    {visibleTransactions.map((transaction) => (
                      <article className="entity-card" key={transaction.id}>
                        <div className="entity-head">
                          <div>
                            <h4>{transaction.description}</h4>
                            <p>{formatEnumLabel(transaction.type)} · {formatDateTime(transaction.occurredAt)}</p>
                          </div>
                          <div className="row-actions">
                            <button className="icon-btn" type="button" onClick={() => openModal('transaction', 'edit', transaction)}>
                              Edit
                            </button>
                            <button className="icon-btn danger" type="button" onClick={() => requestDelete('transaction', transaction)}>
                              Delete
                            </button>
                          </div>
                        </div>

                        <div className="meta-grid">
                          <span>Amount: {formatCurrency(transaction.amount)}</span>
                          <span>Account: {transaction.accountName || 'Unavailable'}</span>
                          <span>Budget: {transaction.budgetName || 'Not linked'}</span>
                        </div>
                      </article>
                    ))}

                    {!visibleTransactions.length && (
                      <EmptyState text={transactionLoading ? 'Loading transactions...' : 'No transactions match the current filters.'} />
                    )}
                  </div>

                  {transactionPage.totalPages > 1 && (
                    <div className="pagination-row">
                      <button
                        className="btn btn-ghost"
                        disabled={transactionFilters.page <= 0 || transactionLoading}
                        type="button"
                        onClick={() => void changeTransactionPage(-1)}
                      >
                        Previous
                      </button>
                      <span className="muted">
                        Page {transactionPage.number + 1} of {transactionPage.totalPages}
                      </span>
                      <button
                        className="btn btn-ghost"
                        disabled={transactionFilters.page >= transactionPage.totalPages - 1 || transactionLoading}
                        type="button"
                        onClick={() => void changeTransactionPage(1)}
                      >
                        Next
                      </button>
                    </div>
                  )}
                </div>
              </PanelSection>
            )}
          </div>
        </section>
      </main>

      {(notice || error) && (
        <div className="status-strip">
          {notice && <div className="notice-box">{notice}</div>}
          {error && <div className="error-box">{error}</div>}
        </div>
      )}

      {loading && (
        <div className="overlay">
          <div className="modal card loading-modal">
            <h3>Loading your dashboard...</h3>
            <p className="muted">Preparing profiles, accounts, budgets, categories and recent activity.</p>
          </div>
        </div>
      )}

      {modalState.type === 'user' && (
        <UserModal
          key={`user-${modalState.mode}-${modalState.payload?.id || 'new'}`}
          mode={modalState.mode}
          payload={modalState.payload}
          onClose={closeModal}
          onSubmit={submitUser}
        />
      )}

      {modalState.type === 'account' && (
        <AccountModal
          key={`account-${modalState.mode}-${modalState.payload?.id || 'new'}`}
          mode={modalState.mode}
          payload={modalState.payload}
          users={users}
          selectedUserId={selectedUserId}
          onClose={closeModal}
          onSubmit={submitAccount}
        />
      )}

      {modalState.type === 'budget' && (
        <BudgetModal
          key={`budget-${modalState.mode}-${modalState.payload?.id || 'new'}`}
          mode={modalState.mode}
          payload={modalState.payload}
          users={users}
          categories={categories}
          selectedUserId={selectedUserId}
          onClose={closeModal}
          onSubmit={submitBudget}
        />
      )}

      {modalState.type === 'category' && (
        <CategoryModal
          key={`category-${modalState.mode}-${modalState.payload?.id || 'new'}`}
          mode={modalState.mode}
          payload={modalState.payload}
          users={users}
          budgets={budgets}
          selectedUserId={selectedUserId}
          onClose={closeModal}
          onSubmit={submitCategory}
        />
      )}

      {modalState.type === 'transaction' && (
        <TransactionModal
          key={`transaction-${modalState.mode}-${modalState.payload?.id || 'new'}`}
          mode={modalState.mode}
          payload={modalState.payload}
          context={transactionModalContext}
          onClose={closeModal}
          onSubmit={submitTransaction}
        />
      )}

      {pendingDelete && (
        <ConfirmDialog
          title={`Delete ${capitalize(pendingDelete.kind)}?`}
          description={`This will permanently delete "${pendingDelete.item.name || pendingDelete.item.description || pendingDelete.item.username}".`}
          confirmLabel="Delete"
          onConfirm={() => void confirmDelete()}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </div>
  );
}

function PanelSection({ title, subtitle, actionLabel, onAction, children }) {
  return (
    <section className="panel-card card">
      <div className="section-head">
        <div>
          <h3>{title}</h3>
          {subtitle && <p className="muted">{subtitle}</p>}
        </div>
        {actionLabel && (
          <button className="btn btn-soft" type="button" onClick={onAction}>
            {actionLabel}
          </button>
        )}
      </div>
      {children}
    </section>
  );
}

function MetricCard({ label, value }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function MetricPill({ label, value }) {
  return (
    <div className="metric-pill">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function CashflowChart({ income, expense, transfer, points, maxValue }) {
  const chartWidth = 620;
  const chartHeight = 176;
  const paddingX = 44;
  const paddingY = 16;
  const innerWidth = chartWidth - paddingX * 2;
  const innerHeight = chartHeight - paddingY * 2;
  const slotWidth = points.length ? innerWidth / points.length : innerWidth;
  const plotPoints = points.length > 1
    ? points.map((point, index) => {
      const x = paddingX + slotWidth * index + slotWidth / 2;
      const incomeHeight = (point.income / maxValue) * innerHeight;
      const expenseHeight = (point.expense / maxValue) * innerHeight;
      const transferHeight = (point.transfer / maxValue) * innerHeight;

      return {
        ...point,
        x,
        incomeHeight,
        expenseHeight,
        transferHeight
      };
    })
    : points.map((point) => {
      const x = paddingX + innerWidth / 2;
      const incomeHeight = (point.income / maxValue) * innerHeight;
      const expenseHeight = (point.expense / maxValue) * innerHeight;
      const transferHeight = (point.transfer / maxValue) * innerHeight;

      return {
        ...point,
        x,
        incomeHeight,
        expenseHeight,
        transferHeight
      };
    });
  const yGuides = [1, 0.75, 0.5, 0.25, 0].map((ratio) => ({
    value: maxValue * ratio,
    y: paddingY + innerHeight * (1 - ratio)
  }));
  const groupWidth = plotPoints.length > 1
    ? Math.min(slotWidth * 0.62, 42)
    : 52;
  const barGap = 4;
  const barWidth = Math.max((groupWidth - barGap * 2) / 3, 7);
  const net = income - expense;

  return (
    <div className="cashflow-chart">
      <div className="cashflow-chart-head">
        <div className="cashflow-chart-copy">
          <strong>Income vs expense trend</strong>
          <span>Last {Math.max(points.length, 1)} active days in the selected profile</span>
        </div>
        <div className="cashflow-legend">
          <span><i className="legend-swatch income" />Income</span>
          <span><i className="legend-swatch expense" />Expense</span>
          <span><i className="legend-swatch transfer" />Transfer</span>
        </div>
      </div>

      {plotPoints.length ? (
        <>
          <div className="cashflow-svg-shell">
            <svg
              className="cashflow-svg"
              viewBox={`0 0 ${chartWidth} ${chartHeight}`}
              role="img"
              aria-label="Cashflow trend chart"
            >
              <defs>
                <linearGradient id="incomeBarGradient" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#58cfb7" />
                  <stop offset="100%" stopColor="#159779" />
                </linearGradient>
                <linearGradient id="expenseBarGradient" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#ff9ab0" />
                  <stop offset="100%" stopColor="#d94d71" />
                </linearGradient>
                <linearGradient id="transferBarGradient" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#8db9ff" />
                  <stop offset="100%" stopColor="#4477dd" />
                </linearGradient>
                <clipPath id="cashflowPlotClip">
                  <rect
                    x={paddingX}
                    y={paddingY}
                    width={innerWidth}
                    height={innerHeight + 2}
                    rx="2"
                  />
                </clipPath>
              </defs>

              <rect
                x={paddingX}
                y={paddingY}
                width={innerWidth}
                height={innerHeight}
                className="cashflow-plot-bg"
                rx="3"
              />

              {yGuides.map((guide) => (
                <g key={guide.y}>
                  <line
                    x1={paddingX}
                    y1={guide.y}
                    x2={chartWidth - paddingX}
                    y2={guide.y}
                    className="cashflow-grid-line"
                  />
                  <text x="6" y={guide.y + 4} className="cashflow-axis-label">
                    {compactCurrency(guide.value)}
                  </text>
                </g>
              ))}

              <line
                x1={paddingX}
                y1={paddingY + innerHeight}
                x2={chartWidth - paddingX}
                y2={paddingY + innerHeight}
                className="cashflow-axis-base"
              />

              <g clipPath="url(#cashflowPlotClip)">
                {plotPoints.map((point) => (
                  <g key={point.date}>
                    <rect
                      x={point.x - groupWidth / 2}
                      y={paddingY + innerHeight - point.incomeHeight}
                      width={barWidth}
                      height={Math.max(point.incomeHeight, point.income > 0 ? 8 : 0)}
                      rx="4"
                      className="cashflow-bar income"
                    />
                    <rect
                      x={point.x - groupWidth / 2 + barWidth + barGap}
                      y={paddingY + innerHeight - point.expenseHeight}
                      width={barWidth}
                      height={Math.max(point.expenseHeight, point.expense > 0 ? 8 : 0)}
                      rx="4"
                      className="cashflow-bar expense"
                    />
                    <rect
                      x={point.x - groupWidth / 2 + (barWidth + barGap) * 2}
                      y={paddingY + innerHeight - point.transferHeight}
                      width={barWidth}
                      height={Math.max(point.transferHeight, point.transfer > 0 ? 8 : 0)}
                      rx="4"
                      className="cashflow-bar transfer"
                    />
                  </g>
                ))}
              </g>
            </svg>
          </div>
          <div
            className="cashflow-label-row"
            style={{ gridTemplateColumns: `repeat(${plotPoints.length}, minmax(0, 1fr))` }}
          >
            {plotPoints.map((point) => (
              <span key={`${point.date}-label`} className="cashflow-date-pill">
                {formatShortDate(point.date)}
              </span>
            ))}
          </div>
        </>
      ) : (
        <EmptyState text="No income or expense points available yet." compact />
      )}

      <div className="stats-row three">
        <MetricPill label="Income" value={formatCurrency(income)} />
        <MetricPill label="Expense" value={formatCurrency(expense)} />
        <MetricPill label="Net" value={formatCurrency(net)} />
      </div>
      <div className="stats-row three">
        <MetricPill label="Transfer" value={formatCurrency(transfer)} />
        <MetricPill label="Chart points" value={points.length} />
        <MetricPill label="Peak day" value={formatCurrency(maxValue)} />
      </div>
    </div>
  );
}

function EmptyState({ text, compact = false }) {
  return <div className={compact ? 'empty-state compact' : 'empty-state'}>{text}</div>;
}

function ModalShell({ title, subtitle, children, onClose }) {
  return (
    <div className="overlay">
      <div className="modal card">
        <header className="modal-head">
          <div>
            <h3>{title}</h3>
            {subtitle && <p className="muted">{subtitle}</p>}
          </div>
          <button className="icon-btn" type="button" onClick={onClose}>Close</button>
        </header>
        {children}
      </div>
    </div>
  );
}

function ConfirmDialog({ title, description, confirmLabel, onConfirm, onCancel }) {
  return (
    <ModalShell
      title={title}
      subtitle={description}
      onClose={onCancel}
    >
      <div className="confirm-dialog-body">
        <div className="notice-box confirm-dialog-note">
          This action cannot be undone.
        </div>
        <div className="modal-actions">
          <button className="btn btn-ghost" type="button" onClick={onCancel}>
            Cancel
          </button>
          <button className="btn btn-danger" type="button" onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </ModalShell>
  );
}

function UserModal({ mode, payload, onClose, onSubmit }) {
  const [form, setForm] = useState({
    username: payload?.username || '',
    email: payload?.email || ''
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function handleSave(event) {
    event.preventDefault();
    setBusy(true);
    setError('');

    try {
      await onSubmit(
        {
          username: form.username.trim(),
          email: form.email.trim()
        },
        mode,
        payload
      );
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <ModalShell
      title={mode === 'create' ? 'Create profile' : 'Edit profile'}
      subtitle="Create a profile to manage accounts, budgets and transactions."
      onClose={onClose}
    >
      <form className="modal-form" onSubmit={handleSave}>
        <label className="field">
          <span>Username</span>
          <input
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
            maxLength={50}
            required
          />
        </label>

        <label className="field">
          <span>Email</span>
          <input
            type="email"
            value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })}
            maxLength={255}
            required
          />
        </label>

        {error && <div className="error-box">{error}</div>}

        <div className="modal-actions">
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy} type="submit">
            {busy ? 'Saving...' : mode === 'create' ? 'Create profile' : 'Save changes'}
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

function AccountModal({ mode, payload, users, selectedUserId, onClose, onSubmit }) {
  const [form, setForm] = useState({
    name: payload?.name || '',
    type: payload?.type || 'CHECKING',
    balance: payload?.balance ?? '0',
    userId: String(selectedUserId || users[0]?.id || '')
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function handleSave(event) {
    event.preventDefault();
    setBusy(true);
    setError('');

    try {
      await onSubmit(
        {
          name: form.name.trim(),
          type: form.type,
          balance: Number(form.balance),
          userId: Number(form.userId)
        },
        mode,
        payload
      );
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <ModalShell
      title={mode === 'create' ? 'Create account' : 'Edit account'}
      subtitle="Add an account for the selected profile."
      onClose={onClose}
    >
      <form className="modal-form" onSubmit={handleSave}>
        <div className="form-grid">
          <label className="field">
            <span>Name</span>
            <input
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              maxLength={50}
              required
            />
          </label>

          <label className="field">
            <span>Type</span>
            <select
              value={form.type}
              onChange={(event) => setForm({ ...form, type: event.target.value })}
            >
              {ACCOUNT_TYPES.map((type) => (
                <option key={type} value={type}>{formatEnumLabel(type)}</option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Balance</span>
            <input
              type="number"
              step="0.01"
              min="0"
              value={form.balance}
              onChange={(event) => setForm({ ...form, balance: event.target.value })}
              required
            />
          </label>

          <label className="field">
            <span>Profile</span>
            <select
              value={form.userId}
              onChange={(event) => setForm({ ...form, userId: event.target.value })}
              required
            >
              <option value="" disabled>Select profile</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>{user.username}</option>
              ))}
            </select>
          </label>
        </div>

        {error && <div className="error-box">{error}</div>}

        <div className="modal-actions">
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy} type="submit">
            {busy ? 'Saving...' : mode === 'create' ? 'Create account' : 'Save changes'}
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

function BudgetModal({ mode, payload, users, categories, selectedUserId, onClose, onSubmit }) {
  const [form, setForm] = useState({
    name: payload?.name || '',
    limitAmount: payload?.limitAmount ?? '0',
    periodStart: payload?.periodStart || '',
    periodEnd: payload?.periodEnd || '',
    userId: String(payload?.userId || selectedUserId || users[0]?.id || ''),
    categoryIds: payload?.categoryIds || []
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const availableCategories = categories.filter(
    (item) => !form.userId || item.userId === Number(form.userId)
  );

  async function handleSave(event) {
    event.preventDefault();
    setBusy(true);
    setError('');

    try {
      await onSubmit(
        {
          name: form.name.trim(),
          limitAmount: Number(form.limitAmount),
          periodStart: form.periodStart,
          periodEnd: form.periodEnd,
          userId: Number(form.userId),
          categoryIds: form.categoryIds
        },
        mode,
        payload
      );
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function toggleCategory(id) {
    setForm((current) => ({
      ...current,
      categoryIds: current.categoryIds.includes(id)
        ? current.categoryIds.filter((item) => item !== id)
        : [...current.categoryIds, id]
    }));
  }

  return (
    <ModalShell
      title={mode === 'create' ? 'Create budget' : 'Edit budget'}
      subtitle="Set a budget and choose which categories it should cover."
      onClose={onClose}
    >
      <form className="modal-form" onSubmit={handleSave}>
        <div className="form-grid">
          <label className="field">
            <span>Name</span>
            <input
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              maxLength={50}
              required
            />
          </label>

          <label className="field">
            <span>Limit amount</span>
            <input
              type="number"
              step="0.01"
              min="0"
              value={form.limitAmount}
              onChange={(event) => setForm({ ...form, limitAmount: event.target.value })}
              required
            />
          </label>

          <label className="field">
            <span>Period start</span>
            <input
              type="date"
              value={form.periodStart}
              onChange={(event) => setForm({ ...form, periodStart: event.target.value })}
              required
            />
          </label>

          <label className="field">
            <span>Period end</span>
            <input
              type="date"
              value={form.periodEnd}
              onChange={(event) => setForm({ ...form, periodEnd: event.target.value })}
              required
            />
          </label>

          <label className="field full-span">
            <span>Profile</span>
            <select
              value={form.userId}
              onChange={(event) => setForm({ ...form, userId: event.target.value, categoryIds: [] })}
              required
            >
              <option value="" disabled>Select profile</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>{user.username}</option>
              ))}
            </select>
          </label>
        </div>

        <div className="picker-block">
          <h4>Categories</h4>
          <div className="chip-cloud">
            {availableCategories.map((item) => (
              <button
                key={item.id}
                className={form.categoryIds.includes(item.id) ? 'chip toggle-chip active' : 'chip toggle-chip'}
                type="button"
                onClick={() => toggleCategory(item.id)}
              >
                {item.name}
              </button>
            ))}
            {!availableCategories.length && (
              <EmptyState text="Create categories for this profile first." compact />
            )}
          </div>
        </div>

        {error && <div className="error-box">{error}</div>}

        <div className="modal-actions">
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy} type="submit">
            {busy ? 'Saving...' : mode === 'create' ? 'Create budget' : 'Save changes'}
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

function CategoryModal({ mode, payload, users, budgets, selectedUserId, onClose, onSubmit }) {
  const [form, setForm] = useState({
    name: payload?.name || '',
    userId: String(payload?.userId || selectedUserId || users[0]?.id || ''),
    budgetIds: payload?.budgetIds || []
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const availableBudgets = budgets.filter(
    (item) => !form.userId || item.userId === Number(form.userId)
  );

  async function handleSave(event) {
    event.preventDefault();
    setBusy(true);
    setError('');

    try {
      await onSubmit(
        {
          name: form.name.trim(),
          userId: Number(form.userId),
          budgetIds: form.budgetIds
        },
        mode,
        payload
      );
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function toggleBudget(id) {
    setForm((current) => ({
      ...current,
      budgetIds: current.budgetIds.includes(id)
        ? current.budgetIds.filter((item) => item !== id)
        : [...current.budgetIds, id]
    }));
  }

  return (
    <ModalShell
      title={mode === 'create' ? 'Create category' : 'Edit category'}
      subtitle="Create a category and attach it to the budgets where it belongs."
      onClose={onClose}
    >
      <form className="modal-form" onSubmit={handleSave}>
        <div className="form-grid">
          <label className="field">
            <span>Name</span>
            <input
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              maxLength={50}
              required
            />
          </label>

          <label className="field">
            <span>Profile</span>
            <select
              value={form.userId}
              onChange={(event) => setForm({ ...form, userId: event.target.value, budgetIds: [] })}
              required
            >
              <option value="" disabled>Select profile</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>{user.username}</option>
              ))}
            </select>
          </label>
        </div>

        <div className="picker-block">
          <h4>Budgets</h4>
          <div className="chip-cloud">
            {availableBudgets.map((item) => (
              <button
                key={item.id}
                className={form.budgetIds.includes(item.id) ? 'chip toggle-chip active' : 'chip toggle-chip'}
                type="button"
                onClick={() => toggleBudget(item.id)}
              >
                {item.name}
              </button>
            ))}
            {!availableBudgets.length && <EmptyState text="Create budgets for this profile first." compact />}
          </div>
        </div>

        {error && <div className="error-box">{error}</div>}

        <div className="modal-actions">
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy} type="submit">
            {busy ? 'Saving...' : mode === 'create' ? 'Create category' : 'Save changes'}
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

function TransactionModal({ mode, payload, context, onClose, onSubmit }) {
  const { users, accounts, budgets, selectedUserId } = context;
  const selectedUser = users.find((item) => item.id === selectedUserId) || null;
  const availableAccounts = selectedUser
    ? accounts.filter((item) => selectedUser.accountIds?.includes(item.id))
    : accounts;
  const availableBudgets = selectedUser
    ? budgets.filter((item) => selectedUser.budgetIds?.includes(item.id))
    : budgets;

  const [form, setForm] = useState({
    occurredAt: toDateTimeInputValue(payload?.occurredAt || new Date().toISOString()),
    amount: payload?.amount != null ? String(payload.amount) : '',
    description: payload?.description || '',
    type: payload?.type || 'EXPENSE',
    budgetId: payload?.budgetId ? String(payload.budgetId) : '',
    accountId: payload?.accountId
      ? String(payload.accountId)
      : String(availableAccounts[0]?.id || '')
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function handleSave(event) {
    event.preventDefault();
    setBusy(true);
    setError('');

    try {
      await onSubmit(
        {
          occurredAt: normalizeDateTime(form.occurredAt),
          amount: Number(form.amount),
          description: form.description.trim(),
          type: form.type,
          budgetId: form.budgetId ? Number(form.budgetId) : null,
          accountId: Number(form.accountId)
        },
        mode,
        payload
      );
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <ModalShell
      title={mode === 'create' ? 'Create transaction' : 'Edit transaction'}
      subtitle="Record income, expenses and transfers for the selected account."
      onClose={onClose}
    >
      <form className="modal-form" onSubmit={handleSave}>
        <div className="form-grid">
          <label className="field">
            <span>Occurred at</span>
            <input
              type="datetime-local"
              value={form.occurredAt}
              onChange={(event) => setForm({ ...form, occurredAt: event.target.value })}
              required
            />
          </label>

          <label className="field">
            <span>Amount</span>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={form.amount}
              onChange={(event) => setForm({ ...form, amount: event.target.value })}
              placeholder="0.00"
              required
            />
          </label>

          <label className="field full-span">
            <span>Description</span>
            <input
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              maxLength={255}
              required
            />
          </label>

          <label className="field">
            <span>Type</span>
            <select
              value={form.type}
              onChange={(event) => setForm({ ...form, type: event.target.value })}
            >
              {TRANSACTION_TYPES.map((type) => (
                <option key={type} value={type}>{formatEnumLabel(type)}</option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Account</span>
            <select
              value={form.accountId}
              onChange={(event) => setForm({ ...form, accountId: event.target.value })}
              required
            >
              <option value="" disabled>Select account</option>
              {availableAccounts.map((account) => (
                <option key={account.id} value={account.id}>{account.name}</option>
              ))}
            </select>
          </label>

          <label className="field full-span">
            <span>Budget</span>
            <select
              value={form.budgetId}
              onChange={(event) => setForm({ ...form, budgetId: event.target.value })}
            >
              <option value="">No budget</option>
              {availableBudgets.map((budget) => (
                <option key={budget.id} value={budget.id}>{budget.name}</option>
              ))}
            </select>
          </label>
        </div>

        {error && <div className="error-box">{error}</div>}

        <div className="modal-actions">
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy} type="submit">
            {busy ? 'Saving...' : mode === 'create' ? 'Create transaction' : 'Save changes'}
          </button>
        </div>
      </form>
    </ModalShell>
  );
}

function matchesText(value, search) {
  const normalizedSearch = search.trim().toLowerCase();
  if (!normalizedSearch) {
    return true;
  }

  return value.toLowerCase().includes(normalizedSearch);
}

function normalizeDateTime(value) {
  if (!value) {
    return '';
  }

  return value.length === 16 ? `${value}:00` : value;
}

function toDateTimeInputValue(value) {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 16);
  }

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function formatCurrency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(amount);
}

function formatDate(value) {
  if (!value) {
    return '—';
  }

  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return value;
  }

  return new Intl.DateTimeFormat('en-CA').format(new Date(value));
}

function formatDateTime(value) {
  if (!value) {
    return '—';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('en-GB', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

function compactCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(Number(value || 0));
}

function formatShortDate(value) {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric'
  }).format(date);
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function formatEnumLabel(value) {
  return String(value || '')
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export default App;
