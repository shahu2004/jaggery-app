import { useEffect, useMemo, useState } from 'react';
import { api, imageUrl } from './api.js';

const TYPES = ['BLOCK', 'CUBE', 'POWDER', 'ORGANIC', 'LIQUID'];
const NEXT_ORDER_STATUSES = {
  PLACED: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
};
const money = (value) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value || 0));
const emptyProduct = { name: '', description: '', type: 'BLOCK', price: '', weightGrams: '1000', categoryId: '', imageUrl: '' };

function readStoredUser() {
  try { return JSON.parse(sessionStorage.getItem('jaggery-user') || 'null'); } catch { return null; }
}

export default function App() {
  const [token, setToken] = useState(() => sessionStorage.getItem('jaggery-token') || '');
  const [user, setUser] = useState(readStoredUser);
  const [page, setPage] = useState('shop');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [type, setType] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sortBy, setSortBy] = useState('id');
  const [direction, setDirection] = useState('desc');
  const [products, setProducts] = useState([]);
  const [sellerProducts, setSellerProducts] = useState([]);
  const [catalogPage, setCatalogPage] = useState(0);
  const [catalogPages, setCatalogPages] = useState(0);
  const [sellerPage, setSellerPage] = useState(0);
  const [sellerPages, setSellerPages] = useState(0);
  const [sellerTotal, setSellerTotal] = useState(0);
  const [catalogTotal, setCatalogTotal] = useState(0);
  const [productsLoading, setProductsLoading] = useState(false);
  const [cart, setCart] = useState([]);
  const [orders, setOrders] = useState([]);
  const [adminOrders, setAdminOrders] = useState([]);
  const [categories, setCategories] = useState([]);
  const [adminUsers, setAdminUsers] = useState([]);
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState({ name: '', email: '', phone: '', password: '' });
  const [productForm, setProductForm] = useState(emptyProduct);
  const [editingId, setEditingId] = useState(null);
  const [imageFile, setImageFile] = useState(null);
  const [categoryName, setCategoryName] = useState('');
  const [busy, setBusy] = useState(false);

  const run = async (work, successMessage = '') => {
    setError('');
    setNotice('');
    setBusy(true);
    try {
      const result = await work();
      if (successMessage) setNotice(successMessage);
      return result;
    } catch (e) {
      setError(e?.message || 'Something went wrong. Please try again.');
      return null;
    } finally {
      setBusy(false);
    }
  };

  const loadSellerProducts = async (pageNumber = sellerPage) => {
    const data = await api(`/api/products/mine?page=${pageNumber}&size=12`, { token });
    setSellerProducts(data?.content || []);
    setSellerPages(data?.totalPages || 0);
    setSellerTotal(data?.totalElements || 0);
  };

  const loadProducts = async () => {
    setProductsLoading(true);
    try {
      const params = new URLSearchParams({ page: String(catalogPage), size: '12', sortBy, direction });
      if (search.trim()) params.set('search', search.trim());
      if (type) params.set('type', type);
      if (categoryId) params.set('categoryId', categoryId);
      if (minPrice !== '') params.set('minPrice', minPrice);
      if (maxPrice !== '') params.set('maxPrice', maxPrice);
      const data = await api(`/api/products?${params.toString()}`);
      setProducts(data?.content || []);
      setCatalogPages(data?.totalPages || 0);
      setCatalogTotal(data?.totalElements || 0);
    } catch (e) {
      setError(e?.message || 'Unable to load products.');
      setProducts([]);
    } finally {
      setProductsLoading(false);
    }
  };

  useEffect(() => { loadProducts(); }, [search, type, categoryId, minPrice, maxPrice, sortBy, direction, catalogPage]);

  useEffect(() => {
    api('/api/categories')
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    if (!token) {
      setCart([]);
      return;
    }
    (async () => {
      try {
        const rows = await api('/api/cart', { token });
        if (page === 'cart') {
          const hydrated = await Promise.all((rows || []).map(async (row) => {
            try { return { ...row, product: await api(`/api/products/${row.productId}`) }; }
            catch { return { ...row, product: null }; }
          }));
          setCart(hydrated);
        } else {
          setCart(rows || []);
        }
      } catch (e) {
        if (page === 'cart') setError(e.message || 'Unable to load cart.');
      }
    })();
  }, [page, token]);

  const loadOrders = async () => {
    if (!token) return;
    const result = await api('/api/orders', { token });
    setOrders(result || []);
  };

  useEffect(() => {
    if (page === 'orders' && token) loadOrders().catch(e => setError(e.message || 'Unable to load orders.'));
  }, [page, token]);

  useEffect(() => {
    if (page === 'seller' && token && user) {
      loadSellerProducts().catch(e => setError(e.message || 'Unable to load seller products.'));
    }
  }, [page, token, user?.userId, sellerPage]);

  useEffect(() => {
    if ((page === 'admin' || page === 'seller') && token) {
      api('/api/categories', { token }).then(setCategories).catch(e => setError(e.message || 'Unable to load categories.'));
    }
    if (page === 'admin' && token && user?.role === 'ADMIN') {
      api('/api/users', { token }).then(setAdminUsers).catch(e => setError(e.message || 'Unable to load user accounts.'));
      api('/api/orders/admin/all', { token }).then(setAdminOrders).catch(e => setError(e.message || 'Unable to load admin orders.'));
    }
  }, [page, token, user?.role]);

  const cartTotal = useMemo(
    () => cart.reduce((sum, row) => sum + Number(row.product?.price || 0) * Number(row.quantity || 0), 0),
    [cart],
  );
  const cartItemCount = cart.reduce((sum, row) => sum + Number(row.quantity || 0), 0);
  const hasActiveFilters = Boolean(search.trim() || type || categoryId || minPrice !== '' || maxPrice !== '');
  const clearCatalogFilters = () => {
    setSearch('');
    setType('');
    setCategoryId('');
    setMinPrice('');
    setMaxPrice('');
    setSortBy('id');
    setDirection('desc');
    setCatalogPage(0);
  };
  const myProducts = sellerProducts.filter(p => Number(p.sellerId) === Number(user?.userId));
  const pendingSellers = adminUsers.filter(account => account.role === 'CUSTOMER');

  const logout = () => {
    sessionStorage.removeItem('jaggery-token');
    sessionStorage.removeItem('jaggery-user');
    setToken('');
    setUser(null);
    setCart([]);
    setOrders([]);
    setPage('shop');
    setNotice('You have signed out.');
    setError('');
  };

  const requireLogin = () => {
    if (token) return true;
    setPage('auth');
    setError('Please sign in first to use your cart or account features.');
    return false;
  };

  const addToCart = async (product) => {
    if (!requireLogin()) return;
    const result = await run(() => api('/api/cart/items', {
      token, method: 'POST', body: { productId: product.id, quantity: 1 },
    }), `${product.name} added to your cart.`);
    if (result) {
      const rows = await api('/api/cart', { token });
      if (page === 'cart') {
        const hydrated = await Promise.all((rows || []).map(async row => {
          try { return { ...row, product: await api(`/api/products/${row.productId}`) }; }
          catch { return { ...row, product: null }; }
        }));
        setCart(hydrated);
      } else {
        setCart(rows || []);
      }
    }
  };

  const authSubmit = async (event) => {
    event.preventDefault();
    const endpoint = authMode === 'register' ? '/api/auth/register' : '/api/auth/login';
    const payload = authMode === 'register'
      ? { name: authForm.name, email: authForm.email, phone: authForm.phone || null, password: authForm.password }
      : { email: authForm.email, password: authForm.password };
    const result = await run(() => api(endpoint, { method: 'POST', body: payload }));
    if (!result) return;
    sessionStorage.setItem('jaggery-token', result.token);
    sessionStorage.setItem('jaggery-user', JSON.stringify(result));
    setToken(result.token);
    setUser(result);
    setPage('shop');
    setAuthForm({ name: '', email: '', phone: '', password: '' });
    setNotice(authMode === 'register'
      ? 'Account created as CUSTOMER. An administrator must approve seller access.'
      : `Welcome back, ${result.name}.`);
    setError('');
  };

  const productSubmit = async (event) => {
    event.preventDefault();
    if (!requireLogin()) return;
    const payload = {
      name: productForm.name.trim(),
      description: productForm.description.trim() || null,
      type: productForm.type,
      price: Number(productForm.price),
      weightGrams: Number(productForm.weightGrams),
      categoryId: productForm.categoryId ? Number(productForm.categoryId) : null,
      imageUrl: productForm.imageUrl.trim() || null,
    };
    const path = editingId ? `/api/products/${editingId}` : '/api/products';
    const result = await run(() => api(path, { token, method: editingId ? 'PUT' : 'POST', body: payload }));
    if (!result) return;
    if (imageFile) {
      const data = new FormData();
      data.append('file', imageFile);
      const uploaded = await run(() => api(`/api/products/${result.id}/image`, { token, method: 'POST', body: data }));
      if (!uploaded) return;
    }
    setProductForm(emptyProduct);
    setEditingId(null);
    setImageFile(null);
    setSellerPage(0);
    await loadProducts();
    await loadSellerProducts(0).catch(e => setError(e.message || 'Product saved, but seller listing did not refresh.'));
    setNotice(editingId ? 'Product updated.' : 'Product saved.');
  };

  const beginEdit = (product) => {
    setEditingId(product.id);
    setProductForm({
      name: product.name || '',
      description: product.description || '',
      type: product.type || 'BLOCK',
      price: String(product.price ?? ''),
      weightGrams: String(product.weightGrams ?? '1000'),
      categoryId: product.categoryId ? String(product.categoryId) : '',
      imageUrl: product.imageUrl || '',
    });
    setImageFile(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const deleteProduct = async (product) => {
    if (!window.confirm(`Soft-delete "${product.name}"?`)) return;
    const result = await run(() => api(`/api/products/${product.id}`, { token, method: 'DELETE' }), 'Product marked inactive.');
    if (result) {
      setSellerPage(0);
      await loadProducts();
      await loadSellerProducts(0).catch(e => setError(e.message || 'Product deleted, but seller listing did not refresh.'));
    }
  };

  const updateCartQuantity = async (productId, quantity) => {
    const result = await run(() => api(`/api/cart/items/${productId}`, {
      token, method: 'PUT', body: { quantity: Number(quantity) },
    }), 'Cart quantity updated.');
    if (result) {
      const rows = await api('/api/cart', { token });
      const hydrated = await Promise.all((rows || []).map(async row => {
        try { return { ...row, product: await api(`/api/products/${row.productId}`) }; }
        catch { return { ...row, product: null }; }
      }));
      setCart(hydrated);
    }
  };

  const removeCartItem = async (productId) => {
    const result = await run(() => api(`/api/cart/items/${productId}`, { token, method: 'DELETE' }), 'Item removed from cart.');
    if (result) {
      const rows = await api('/api/cart', { token });
      const hydrated = await Promise.all((rows || []).map(async row => {
        try { return { ...row, product: await api(`/api/products/${row.productId}`) }; }
        catch { return { ...row, product: null }; }
      }));
      setCart(hydrated);
    }
  };

  const checkout = async () => {
    const result = await run(() => api('/api/orders/checkout', { token, method: 'POST' }), 'Test checkout complete. No live payment was processed.');
    if (result) {
      setCart([]);
      await loadOrders().catch(e => setError(e.message));
      setPage('orders');
    }
  };

  const cancelOrder = async (id) => {
    const result = await run(() => api(`/api/orders/${id}/cancel`, { token, method: 'POST' }), 'Order cancelled.');
    if (result) await loadOrders().catch(e => setError(e.message));
  };

  const changeOrderStatus = async (id, status) => {
    const result = await run(
      () => api(`/api/orders/${id}/status`, { token, method: 'PATCH', body: { status } }),
      `Order #${id} updated to ${status}.`,
    );
    if (result) {
      const refreshed = await api('/api/orders/admin/all', { token }).catch(e => {
        setError(e.message || 'Order updated, but the admin list did not refresh.');
        return null;
      });
      if (refreshed) setAdminOrders(refreshed);
    }
  };

  const approveSeller = async (id) => {
    const result = await run(
      () => api(`/api/users/${id}/approve-seller`, { token, method: 'POST' }),
      'Seller access approved. The user must sign in again to receive the updated role.',
    );
    if (result) {
      const refreshed = await api('/api/users', { token }).catch(() => adminUsers);
      setAdminUsers(refreshed || []);
    }
  };

  const createCategory = async (event) => {
    event.preventDefault();
    const result = await run(() => api('/api/categories', { token, method: 'POST', body: { name: categoryName.trim() } }), 'Category created.');
    if (result) {
      setCategoryName('');
      const updated = await api('/api/categories', { token }).catch(() => categories);
      setCategories(updated || []);
    }
  };

  const navButton = (id, label) => (
    <button className={page === id ? 'nav-link active' : 'nav-link'} key={id} onClick={() => { setPage(id); setError(''); setNotice(''); }}>
      {label}
    </button>
  );

  const ProductCard = ({ product, seller = false }) => (
    <article className="product-card" key={product.id}>
      <div className="product-image">
        {product.imageUrl
          ? <img src={imageUrl(product.imageUrl)} alt={product.name} loading="lazy" />
          : <div className="image-placeholder" aria-label="Jaggery product image placeholder"><span aria-hidden="true">🌾</span><small>Jaggery pantry</small></div>}
        <span className="type-badge">{String(product.type || 'JAGGERY').replaceAll('_', ' ')}</span>
        {product.weightGrams && <span className="weight-badge">{product.weightGrams} g</span>}
      </div>
      <div className="product-card-body">
        <span className="product-kicker">{product.categoryName || 'Traditional pantry'}</span>
        <div className="product-heading">
          <h3>{product.name}</h3>
          <strong>{money(product.price)}</strong>
        </div>
        <p className="muted product-description">{product.description || 'A versatile jaggery staple for your kitchen.'}</p>
        <div className="product-meta"><span>Net weight</span><strong>{product.weightGrams ? String(product.weightGrams) + ' g' : 'See details'}</strong></div>
        {seller
          ? <div className="card-actions">
              <button className="button button-secondary" onClick={() => beginEdit(product)}>Edit</button>
              <button className="button button-danger" onClick={() => deleteProduct(product)}>Delete listing</button>
            </div>
          : <button className="button button-primary full add-cart-button" onClick={() => addToCart(product)} disabled={busy}>
              <span>Add to cart</span><span aria-hidden="true">＋</span>
            </button>}
      </div>
    </article>
  );

  return (
    <div className="app-shell">
      <div className="announcement-bar">
        <span><span aria-hidden="true">✦</span> A little sweetness, rooted in tradition</span>
        <span className="announcement-note">Marketplace demo · Test checkout only</span>
      </div>
      <header className="topbar">
        <button className="brand" onClick={() => setPage('shop')} aria-label="Go to Jaggery home">
          <span className="brand-mark">J</span>
          <span className="brand-copy"><strong>Jaggery</strong><small>From farm to family</small></span>
        </button>
        <nav className="main-nav" aria-label="Primary navigation">
          {navButton('shop', 'Shop')}
          {token && navButton('orders', 'My orders')}
          {token && ['SELLER', 'ADMIN'].includes(user?.role) && navButton('seller', 'Seller studio')}
          {token && user?.role === 'ADMIN' && navButton('admin', 'Admin')}
        </nav>
        <div className="account-area">
          {token && <button className={'cart-link ' + (page === 'cart' ? 'active' : '')} onClick={() => setPage('cart')} aria-label={'Open cart, ' + cartItemCount + ' items'}>
            <span aria-hidden="true">♧</span><span>Cart</span><span className="cart-count">{cartItemCount}</span>
          </button>}
          {token
            ? <><span className="user-chip"><span className="user-avatar" aria-hidden="true">{(user?.name || 'U').trim().charAt(0).toUpperCase()}</span><span className="user-copy">{user?.name || 'Signed in'}<small>{user?.role}</small></span></span><button className="button button-secondary compact" onClick={logout}>Sign out</button></>
            : <><button className="header-signin" onClick={() => { setPage('auth'); setAuthMode('login'); }}>Sign in</button><button className="button button-primary compact header-account-cta" onClick={() => { setPage('auth'); setAuthMode('register'); }}>Create account</button></>}
        </div>
      </header>

      <main className="page-wrap">
        {notice && <div className="notice success" role="status"><span>✓</span>{notice}<button onClick={() => setNotice('')} aria-label="Dismiss message">×</button></div>}
        {error && <div className="notice error" role="alert"><span>!</span>{error}<button onClick={() => setError('')} aria-label="Dismiss error">×</button></div>}

        {page === 'shop' && <>
          <section className="hero">
            <div className="hero-copy">
              <span className="eyebrow">ROOTED IN INDIAN TRADITION</span>
              <h1>Sweetness that<br/><em>brings us home.</em></h1>
              <p>Explore jaggery for everyday chai, family recipes and the little rituals that make a kitchen feel like home.</p>
              <button className="button button-light hero-cta" onClick={() => document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' })}>
                Shop the collection <span aria-hidden="true">↗</span>
              </button>
              <div className="hero-proof"><span><strong>01</strong> Browse by format</span><span><strong>02</strong> Compare your options</span></div>
            </div>
            <div className="hero-visual" aria-label="Decorative illustration of jaggery blocks">
              <div className="sun-disc"></div><div className="jaggery-block block-one"></div><div className="jaggery-block block-two"></div><div className="leaf leaf-one"></div><div className="leaf leaf-two"></div>
              <span className="hero-seal">A TASTE<br/>OF HOME</span>
              <span className="hero-visual-caption">THE EVERYDAY PANTRY</span>
            </div>
            <div className="hero-bottom"><span>Traditional formats</span><span>Made for everyday kitchens</span><span>Test checkout · No payment collected</span></div>
          </section>

          <section className="value-strip" aria-label="Marketplace highlights">
            <div className="value-item"><span className="value-icon" aria-hidden="true">01</span><div><strong>Find your format</strong><small>Blocks, cubes, powder and more</small></div></div>
            <div className="value-item"><span className="value-icon" aria-hidden="true">02</span><div><strong>Compare with ease</strong><small>Explore price, type and weight</small></div></div>
            <div className="value-item"><span className="value-icon" aria-hidden="true">03</span><div><strong>Simple demo checkout</strong><small>Try the order flow without payment</small></div></div>
          </section>

          <section className="shop-by-type" aria-labelledby="shop-by-type-title">
            <div className="shop-by-type-heading"><span className="eyebrow">FIND YOUR FAVOURITE</span><h2 id="shop-by-type-title">Shop by <em>format.</em></h2></div>
            <div className="type-chip-list">
              <button className={!type ? 'type-chip active' : 'type-chip'} onClick={() => { setType(''); setCatalogPage(0); document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' }); }}>All products <span>↗</span></button>
              {TYPES.map((item, index) => <button key={item} className={type === item ? 'type-chip active' : 'type-chip'} onClick={() => { setType(type === item ? '' : item); setCatalogPage(0); document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' }); }}><span className="type-chip-number">0{index + 1}</span>{item.toLowerCase().replace(/\b\w/g, char => char.toUpperCase())}<span aria-hidden="true">↗</span></button>)}
            </div>
          </section>

          <section id="catalog" className="catalog-section">
            <div className="section-heading catalog-title-row">
              <div><span className="eyebrow">THE JAGGERY MARKETPLACE</span><h2>Find something <em>good.</em></h2><p className="catalog-subtitle">Search the collection, refine by type or category, and find the right fit for your kitchen.</p></div>
              <span className="count-label"><strong>{catalogTotal}</strong> {catalogTotal === 1 ? 'product' : 'products'} found</span>
            </div>
            <div className="filters">
              <label className="search-field"><span aria-hidden="true">⌕</span><input value={search} onChange={e => { setSearch(e.target.value); setCatalogPage(0); }} placeholder="Search jaggery products..." aria-label="Search products"/></label>
              <select value={type} onChange={e => { setType(e.target.value); setCatalogPage(0); }} aria-label="Filter product type"><option value="">All formats</option>{TYPES.map(t => <option key={t}>{t}</option>)}</select>
              <select value={categoryId} onChange={e => { setCategoryId(e.target.value); setCatalogPage(0); }} aria-label="Filter by category"><option value="">All categories</option>{categories.map(category => <option value={category.id} key={category.id}>{category.name}</option>)}</select>
              <input className="price-filter" type="number" min="0" value={minPrice} onChange={e => { setMinPrice(e.target.value); setCatalogPage(0); }} placeholder="Min ₹" aria-label="Minimum price"/>
              <input className="price-filter" type="number" min="0" value={maxPrice} onChange={e => { setMaxPrice(e.target.value); setCatalogPage(0); }} placeholder="Max ₹" aria-label="Maximum price"/>
              <select value={sortBy} onChange={e => { setSortBy(e.target.value); setCatalogPage(0); }} aria-label="Sort products"><option value="id">Sort: latest</option><option value="name">Sort: name</option><option value="price">Sort: price</option><option value="createdAt">Sort: date added</option></select>
              <button className="button button-secondary compact sort-direction" onClick={() => { setDirection(direction === 'asc' ? 'desc' : 'asc'); setCatalogPage(0); }} aria-label="Toggle sort direction">{direction === 'asc' ? '↑ Ascending' : '↓ Descending'}</button>
            </div>
            {hasActiveFilters && <div className="active-filter-row"><span>Showing results for your selection</span><button type="button" onClick={clearCatalogFilters}>Clear all filters ×</button></div>}
            {productsLoading
              ? <div className="product-grid skeleton-grid" aria-label="Loading products">{[0, 1, 2, 3].map(item => <div key={item} className="skeleton-card"><div className="skeleton-image"></div><div className="skeleton-line"></div><div className="skeleton-line short"></div><div className="skeleton-button"></div></div>)}</div>
              : products.length
                ? <div className="product-grid">{products.map(p => <ProductCard key={p.id} product={p}/>)}</div>
                : <div className="empty-state catalog-empty">
                    <div className="empty-state-icon" aria-hidden="true">{hasActiveFilters ? '⌕' : '🌾'}</div>
                    <span className="eyebrow">{hasActiveFilters ? 'TRY ANOTHER SEARCH' : 'THE SHELF IS READY'}</span>
                    <h3>{hasActiveFilters ? 'No matches this time.' : 'Your next favourite starts here.'}</h3>
                    <p>{hasActiveFilters ? 'We could not find a product that matches every filter. Clear a filter or try a different search.' : 'Products will show up here as soon as a seller publishes a listing. Browse the formats above or come back shortly.'}</p>
                    <div className="empty-state-actions">
                      {hasActiveFilters && <button className="button button-secondary" onClick={clearCatalogFilters}>Clear filters</button>}
                      {['SELLER', 'ADMIN'].includes(user?.role) && !hasActiveFilters && <button className="button button-primary" onClick={() => setPage('seller')}>Open Seller Studio <span aria-hidden="true">↗</span></button>}
                      {!token && !hasActiveFilters && <button className="button button-secondary" onClick={() => { setPage('auth'); setAuthMode('register'); }}>Create an account</button>}
                    </div>
                  </div>}
            {!productsLoading && catalogPages > 1 && <div className="pagination"><button className="button button-secondary compact" disabled={catalogPage <= 0} onClick={() => setCatalogPage(p => Math.max(0, p - 1))}>Previous</button><span>Page {catalogPage + 1} of {catalogPages}</span><button className="button button-secondary compact" disabled={catalogPage + 1 >= catalogPages} onClick={() => setCatalogPage(p => Math.min(catalogPages - 1, p + 1))}>Next</button></div>}
          </section>
        </>}

        {page === 'auth' && <section className="auth-layout">
          <div className="auth-aside"><span className="eyebrow">WELCOME TO THE MARKETPLACE</span><h1>Your kitchen's<br/><em>little upgrade.</em></h1><p>Sign in to save your cart, place test orders and follow your purchases.</p><div className="auth-decoration">J</div></div>
          <form className="form-panel" onSubmit={authSubmit}>
            <div className="section-heading"><div><span className="eyebrow">YOUR ACCOUNT</span><h2>{authMode === 'login' ? 'Welcome back' : 'Create account'}</h2></div></div>
            {authMode === 'register' && <label>Full name<input required maxLength="100" value={authForm.name} onChange={e => setAuthForm({ ...authForm, name: e.target.value })} autoComplete="name"/></label>}
            <label>Email address<input type="email" required value={authForm.email} onChange={e => setAuthForm({ ...authForm, email: e.target.value })} autoComplete="email"/></label>
            {authMode === 'register' && <label>Phone (optional)<input inputMode="numeric" maxLength="10" value={authForm.phone} onChange={e => setAuthForm({ ...authForm, phone: e.target.value })} placeholder="10-digit mobile"/></label>}
            <label>Password<input type="password" required minLength="8" value={authForm.password} onChange={e => setAuthForm({ ...authForm, password: e.target.value })} autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}/></label>
            <button className="button button-primary full" disabled={busy}>{busy ? 'Please wait…' : authMode === 'login' ? 'Sign in' : 'Create account'}</button>
            <p className="form-switch">{authMode === 'login' ? 'New here?' : 'Already have an account?'} <button type="button" onClick={() => setAuthMode(authMode === 'login' ? 'register' : 'login')}>{authMode === 'login' ? 'Create an account' : 'Sign in'}</button></p>
            {authMode === 'register' && <p className="hint">New accounts start as CUSTOMER. Seller access requires administrator approval.</p>}
          </form>
        </section>}

        {page === 'cart' && <section className="content-section">
          <div className="section-heading"><div><span className="eyebrow">YOUR SELECTION</span><h2>Your <em>cart.</em></h2></div><span className="count-label">{cart.length} line items</span></div>
          {!cart.length ? <div className="empty-state"><span>🧺</span><h3>Your cart is empty</h3><p>Browse the marketplace and add a little sweetness.</p><button className="button button-primary" onClick={() => setPage('shop')}>Browse products</button></div>
            : <div className="cart-layout"><div className="cart-list">{cart.map(row => <article className="cart-row" key={row.productId}>
                <div className="cart-thumb">{row.product?.imageUrl ? <img src={imageUrl(row.product.imageUrl)} alt=""/> : '🌾'}</div>
                <div className="cart-product"><strong>{row.product?.name || `Product #${row.productId}`}</strong><small>{row.product ? money(row.product.price) : 'Product details unavailable'}</small></div>
                <label className="quantity-control">Qty<input type="number" min="1" max="99" value={row.quantity} onChange={e => { const q=Number(e.target.value); if(q>=1&&q<=99) updateCartQuantity(row.productId,q); }}/></label>
                <strong className="line-price">{money(Number(row.product?.price || 0) * row.quantity)}</strong>
                <button className="icon-button" onClick={() => removeCartItem(row.productId)} aria-label="Remove item">×</button>
              </article>)}</div>
              <aside className="order-summary"><span className="eyebrow">ORDER SUMMARY</span><h3>Almost yours.</h3><div className="summary-line"><span>Items subtotal (estimate)</span><strong>{money(cartTotal)}</strong></div><p className="hint">The server recalculates prices from Product Service at checkout. This MVP uses a simulated checkout; no payment is collected.</p><button className="button button-primary full" disabled={busy} onClick={checkout}>Place test order</button></aside>
            </div>}
        </section>}

        {page === 'orders' && <section className="content-section">
          <div className="section-heading"><div><span className="eyebrow">YOUR PURCHASES</span><h2>Order <em>history.</em></h2></div></div>
          {!orders.length ? <div className="empty-state"><span>📦</span><h3>No orders yet</h3><p>Your completed test checkouts will appear here.</p><button className="button button-primary" onClick={() => setPage('shop')}>Explore products</button></div>
            : <div className="orders-list">{orders.map(order => <article className="order-card" key={order.id}>
                <div className="order-card-top"><div><small>ORDER #{order.id}</small><h3>{new Date(order.createdAt).toLocaleString()}</h3></div><span className={`status-pill status-${String(order.status).toLowerCase()}`}>{order.status}</span></div>
                <div className="order-lines">{(order.items || []).map((item,i) => <div className="order-line" key={`${order.id}-${i}`}><span>{item.productName} × {item.quantity}</span><strong>{money(item.lineTotal)}</strong></div>)}</div>
                <div className="order-card-bottom"><span>Total</span><strong>{money(order.totalAmount)}</strong>{order.status === 'PLACED' && <button className="button button-secondary compact" onClick={() => cancelOrder(order.id)}>Cancel order</button>}</div>
              </article>)}</div>}
        </section>}

        {page === 'seller' && <section className="content-section">
          {user?.role === 'CUSTOMER' && <div className="notice warning">Your account is a customer. Seller studio becomes available after an administrator approves your account.</div>}
          {['SELLER','ADMIN'].includes(user?.role) && <>
            <div className="section-heading"><div><span className="eyebrow">SELLER STUDIO</span><h2>Manage your <em>products.</em></h2></div><span className="count-label">{sellerTotal} products</span></div>
            <div className="seller-layout">
              <form className="form-panel product-editor" onSubmit={productSubmit}>
                <h3>{editingId ? `Edit product #${editingId}` : 'Add a product'}</h3>
                <label>Product name<input required maxLength="150" value={productForm.name} onChange={e => setProductForm({ ...productForm, name: e.target.value })}/></label>
                <label>Description<textarea maxLength="1000" rows="3" value={productForm.description} onChange={e => setProductForm({ ...productForm, description: e.target.value })}/></label>
                <div className="form-row"><label>Type<select value={productForm.type} onChange={e => setProductForm({ ...productForm, type: e.target.value })}>{TYPES.map(t => <option key={t}>{t}</option>)}</select></label><label>Price (₹)<input required type="number" min="0.01" step="0.01" value={productForm.price} onChange={e => setProductForm({ ...productForm, price: e.target.value })}/></label></div>
                <div className="form-row"><label>Weight (g)<input required type="number" min="1" value={productForm.weightGrams} onChange={e => setProductForm({ ...productForm, weightGrams: e.target.value })}/></label><label>Category<select value={productForm.categoryId} onChange={e => setProductForm({ ...productForm, categoryId: e.target.value })}><option value="">No category</option>{categories.map(c => <option value={c.id} key={c.id}>{c.name}</option>)}</select></label></div>
                <label>Image URL (optional)<input value={productForm.imageUrl} onChange={e => setProductForm({ ...productForm, imageUrl: e.target.value })} placeholder="https://…"/></label>
                <label>Upload image (optional)<input type="file" accept="image/jpeg,image/png,image/webp" onChange={e => setImageFile(e.target.files?.[0] || null)}/></label>
                <div className="card-actions"><button className="button button-primary" disabled={busy}>{editingId ? 'Save changes' : 'Create product'}</button>{editingId && <button type="button" className="button button-secondary" onClick={() => {setEditingId(null);setProductForm(emptyProduct);setImageFile(null);}}>Cancel edit</button>}</div>
                <p className="hint">Server-side seller ownership checks are required for every write.</p>
              </form>
              <div className="seller-products"><h3>Your products</h3>{myProducts.length ? <div className="product-grid">{myProducts.map(p => <ProductCard key={p.id} product={p} seller/>)}</div> : <p className="muted">No active products are owned by your account.</p>}{sellerPages > 1 && <div className="pagination"><button className="button button-secondary compact" disabled={sellerPage <= 0} onClick={() => setSellerPage(p => Math.max(0, p - 1))}>Previous</button><span>Page {sellerPage + 1} of {sellerPages}</span><button className="button button-secondary compact" disabled={sellerPage + 1 >= sellerPages} onClick={() => setSellerPage(p => Math.min(sellerPages - 1, p + 1))}>Next</button></div>}</div>
            </div>
          </>}
        </section>}

        {page === 'admin' && user?.role === 'ADMIN' && <section className="content-section">
          <div className="section-heading"><div><span className="eyebrow">ADMINISTRATION</span><h2>Manage <em>categories.</em></h2></div></div>
          <div className="admin-layout">
            <form className="form-panel" onSubmit={createCategory}><h3>Create category</h3><label>Category name<input required maxLength="100" value={categoryName} onChange={e => setCategoryName(e.target.value)}/></label><button className="button button-primary" disabled={busy}>Create category</button></form>
            <div className="category-panel"><h3>Current categories</h3>{categories.length ? categories.map(c => <div className="category-row" key={c.id}><span>{c.name}</span><small>#{c.id}</small></div>) : <p className="muted">No categories yet.</p>}</div>
            <div className="category-panel seller-approval-panel"><h3>Seller approvals</h3><p className="hint">Review customer accounts before granting seller access.</p>{pendingSellers.length ? pendingSellers.map(account => <div className="category-row approval-row" key={account.id}><span><strong>{account.name}</strong><small>{account.email}</small></span><button type="button" className="button button-secondary compact" disabled={busy} onClick={() => approveSeller(account.id)}>Approve seller</button></div>) : <p className="muted">No customer accounts awaiting seller approval.</p>}</div>
            <div className="category-panel admin-orders-panel"><h3>All orders</h3><p className="hint">Review customer orders and apply only permitted status transitions.</p>{adminOrders.length ? adminOrders.map(order => <div className="category-row approval-row" key={order.id}><span><strong>Order #{order.id} · Customer #{order.userId}</strong><small>{(order.items || []).map(item => `${item.productName} × ${item.quantity}`).join(', ')} · {money(order.totalAmount)}</small></span><select aria-label={`Update status for order ${order.id}`} value={order.status} disabled={busy || !(NEXT_ORDER_STATUSES[order.status] || []).length} onChange={e => changeOrderStatus(order.id, e.target.value)}><option value={order.status}>{order.status}</option>{(NEXT_ORDER_STATUSES[order.status] || []).map(status => <option key={status} value={status}>{status}</option>)}</select></div>) : <p className="muted">No orders have been placed yet.</p>}</div>
          </div>
        </section>}

        <footer className="footer"><div className="footer-brand"><span className="brand-mark">J</span><span><strong>Jaggery</strong><small>From farm to family</small></span></div><p>A portfolio MVP built with React, Spring Boot and MySQL.</p><span className="footer-note">Test checkout only · No real payment processing</span></footer>
      </main>
    </div>
  );
}
