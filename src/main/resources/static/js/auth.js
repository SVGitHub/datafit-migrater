async function initAuth(){
  const mount = document.getElementById('authArea');
  try {
    const me = await api('/api/me');
    window.CurrentUser = me;
    mount.innerHTML = `<span class="badge">Signed in as ${escapeHtml(me.email||'user')}</span> <a class="badge" href="/logout">Logout</a>` + (me.admin?` <span class="badge">Admin</span>`:'');
    if(!me.admin){
      document.querySelectorAll('[data-admin-only]').forEach(e=>e.style.display='none');
    }
  } catch(e) {
    mount.innerHTML = `<a class="badge" href="/oauth2/authorization/google">Sign in with Google</a>`;
  }
}
