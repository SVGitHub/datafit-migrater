async function renderAdmin(){
  const host = document.getElementById('tab-admin') || document.body;
  host.innerHTML = '';
  const box = document.createElement('div');
  box.className='card';
  box.innerHTML = `<h3>Admin: Project Configuration & User Access</h3>
  <div class="row"><div id="adminProjectBox"></div></div>
  <div id="adminContent"></div>`;
  host.appendChild(box);
  const sel = projectSelect('adminProjectSel'); box.querySelector('#adminProjectBox').appendChild(sel); await fillProjectSelect(sel.querySelector('select'));
  sel.querySelector('select').addEventListener('change', async (e)=>{ await loadProject(e.target.value); });
  if(Store.projectId) await loadProject(Store.projectId); else if(document.querySelector('#adminProjectSel select option')) { await loadProject(document.querySelector('#adminProjectSel select').value); }
  async function loadProject(pid){
    try{
      const res = await api('/api/projects');
      const p = res.find(x=>x.id===pid);
      // fetch full project via backend? we only have minimal fields; call a dedicated endpoint (not implemented) - instead load knownHosts via projects list maybe not available.
      // For now, provide form to set knownHosts via PUT /api/projects
      const adminContent = box.querySelector('#adminContent');
      adminContent.innerHTML = `<div class="grid">
        <div class="col-6"><label>Known Hosts (paste content)</label><textarea id="knownHosts" rows="10" style="width:100%"></textarea></div>
        <div class="col-6"><label>Manage Users</label><div id="userAccessBox"></div></div>
      </div><div class="row"><button class="primary" id="saveProject">Save</button></div>`;
      // load user access for project
      await loadUserAccess(pid);
      adminContent.querySelector('#saveProject').addEventListener('click', async ()=>{
        const kh = adminContent.querySelector('#knownHosts').value;
        try{ await api(`/api/projects?projectId=${pid}`,'PUT',{ knownHosts: kh }); toast('Saved', true); } catch(e){ toast('Save failed: '+e.message,false); }
      });
    }catch(e){ toast('Failed to load project: '+e.message,false); }
  }

  async function loadUserAccess(pid){
    const box = document.getElementById('userAccessBox');
    box.innerHTML = '<div><button id="btnAddUser" class="primary">Add User</button><div id="usersList"></div></div>';
    box.querySelector('#btnAddUser').addEventListener('click', async ()=>{
      const email = prompt('User email'); if(!email) return;
      const role = prompt('Role (ADMIN/USER)', 'USER');
      try{ await api('/api/admin/user-access', 'POST', { email, role, projectId: pid }); toast('Added', true); await loadUserAccess(pid);}catch(e){ toast('Add failed: '+e.message,false); }
    });
    try{
      const list = await api(`/api/admin/user-access?projectId=${pid}`);
      const ul = document.createElement('div');
      ul.innerHTML = list.map(u=>`<div style="display:flex;gap:8px;align-items:center"><div style="flex:1">${u.email} <small>(${u.roleName})</small></div><button data-id="${u.id}" class="ghost btnRemove">Remove</button></div>`).join('');
      box.querySelector('#usersList').innerHTML=''; box.querySelector('#usersList').appendChild(ul);
      box.querySelectorAll('.btnRemove').forEach(b=>b.addEventListener('click', async (e)=>{
        const id = e.target.getAttribute('data-id');
        if(!confirm('Remove access?')) return;
        try{ await api('/api/admin/user-access?id='+id,'DELETE'); toast('Removed', true); await loadUserAccess(pid); }catch(err){ toast('Remove failed: '+err.message,false); }
      }));
    }catch(err){ box.querySelector('#usersList').textContent = 'Failed to load users: '+err.message; }
  }
}

window.addEventListener('DOMContentLoaded', ()=>{ if(document.getElementById('tab-admin')) renderAdmin(); });
