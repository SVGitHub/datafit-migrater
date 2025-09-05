async function api(url, method='GET', data=null){
  const opts = { method, headers: { 'Content-Type':'application/json' } };
  if(data) opts.body = JSON.stringify(data);
  const res = await fetch(url, opts);
  if(!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  const ct = res.headers.get('content-type')||'';
  return ct.includes('application/json')? res.json() : res.text();
}
function el(html){ const d=document.createElement('div'); d.innerHTML=html.trim(); return d.firstElementChild; }
function escapeHtml(s){ return String(s).replace(/[<>&]/g, c=>({'<':'&lt;','>':'&gt;','&':'&amp;'}[c])); }
function toast(msg, ok=true){ const t=el(`<div style="position:fixed;right:16px;bottom:16px;padding:10px;border:1px solid #ddd;border-radius:8px;background:${ok?'#e8fff0':'#ffecec'}">${escapeHtml(msg)}</div>`); document.body.appendChild(t); setTimeout(()=>t.remove(),2500); }
function spinner(){ return `<span class="badge">Working…</span>`; }
const Store = { projectId:null, projects:[] };
async function ensureProjects(){ if(Store.projects.length) return Store.projects; const items = await api('/api/projects'); Store.projects = items; if(!Store.projectId && items.length) Store.projectId = items[0].id; return items; }
function projectSelect(id='projectSel'){ return el(`<div><label>Project</label><select id="${id}"></select></div>`); }
async function fillProjectSelect(sel){ const items = await ensureProjects(); sel.innerHTML = items.map(p=>`<option value="${p.id}" ${p.id===Store.projectId?'selected':''}>${escapeHtml(p.name)} (${escapeHtml(String(p.dbType))})</option>`).join(''); sel.addEventListener('change', e=> Store.projectId = e.target.value); }

async function renderJobs(){
  const host = document.getElementById('tab-jobs'); if(!host) return;
  host.innerHTML = '';
  const header = el(`<div class="row" style="justify-content:space-between;margin-bottom:8px"><h2 style="margin:0">Jobs</h2><div class="row"><button class="ghost" id="btnRefresh">Refresh</button></div></div>`);
  host.appendChild(header);
  const form = el(`<div class="card"><h3>Create Job</h3><div class="grid">
    <div class="col-4" id="projectBox"></div>
    <div class="col-4"><label>Source</label><select id="sourceType"><option>LOCAL</option><option>SFTP</option><option>S3</option></select></div>
    <div class="col-4"><label>Folder / Path</label><input id="folder" placeholder="./data"/></div>
    <div class="col-4"><label>File Glob</label><input id="glob" placeholder="*.parquet,*.csv,*.xlsx"/></div>
    <div class="col-4"><label>Target Schema</label><input id="schema" placeholder="public"/></div>
    <div class="col-4"><label>Target Table</label><input id="table" placeholder="orders"/></div>
  </div><div class="hr"></div><div class="row"><button class="primary" id="btnStart">Start Job</button><span id="startStatus"></span></div></div>`);
  host.appendChild(form);
  const projSel = projectSelect('jobProjectSel'); form.querySelector('#projectBox').appendChild(projSel); await fillProjectSelect(projSel.querySelector('select'));
  form.querySelector('#btnStart').addEventListener('click', async ()=>{
    const payload = { projectId: Store.projectId, sourceType: form.querySelector('#sourceType').value, folder: form.querySelector('#folder').value, glob: form.querySelector('#glob').value, targetSchema: form.querySelector('#schema').value, targetTable: form.querySelector('#table').value };
    try{ form.querySelector('#startStatus').innerHTML = spinner(); const id = await api('/api/jobs','POST',payload); form.querySelector('#startStatus').innerHTML = `<span class="badge">Started ${id}</span>`; await reloadJobs(); }catch(e){ form.querySelector('#startStatus').textContent = e.message; }
  });
  const tbl = el(`<div class="card"><h3>Recent Jobs</h3><table><thead><tr><th>ID</th><th>Project</th><th>Target</th><th>Source</th><th>Status</th><th>Totals</th><th>Started</th><th>Finished</th><th>Details</th></tr></thead><tbody id="jobsBody"></tbody></table></div>`);
  host.appendChild(tbl);
  header.querySelector('#btnRefresh').addEventListener('click', reloadJobs);
  await reloadJobs();
  async function reloadJobs(){
    try {
      const items = await api(`/api/jobs?projectId=${Store.projectId}`);
      const body = tbl.querySelector('#jobsBody');
      body.innerHTML = items.map(j=>`<tr>
        <td>${escapeHtml(j.id)}</td>
        <td>${escapeHtml(j.project?.name||'')}</td>
        <td>${escapeHtml([j.targetSchema,j.targetTable].filter(Boolean).join('.'))}</td>
        <td>${escapeHtml(j.sourceType||'')}</td>
        <td>${escapeHtml(j.status||'')}</td>
        <td>${escapeHtml((j.successRows||0)+"/"+(j.totalRows||0))}</td>
        <td>${escapeHtml(j.startedAt||'')}</td>
        <td>${escapeHtml(j.finishedAt||'')}</td>
        <td><button class="ghost" onclick="showJobDetails('${j.id}')">Details</button></td>
      </tr>`).join('');
    } catch(e){ toast(e.message,false); }
  }
}

async function showJobDetails(jobId){
  try{
    const job = await api('/api/jobs/'+jobId);
    const errors = await api('/api/jobs/'+jobId+'/errors');
    const modal = document.createElement('div');
    modal.innerHTML = `<div class="card"><h3>Job Details - ${jobId}</h3><pre>Project: ${job.project?.name}\nStatus: ${job.status}\nTotals: ${job.successRows||0}/${job.totalRows||0}\nStarted: ${job.startedAt}\nFinished: ${job.finishedAt}</pre><h4>Errors (${errors.length})</h4><table><thead><tr><th>Row</th><th>Reason</th></tr></thead><tbody>${errors.map(e=>`<tr><td>${e.rowNum||''}</td><td>${e.reason||''}</td></tr>`).join('')}</tbody></table><div class="row" style="margin-top:8px"><a class="badge" href="/api/jobs/${jobId}/error-file" target="_blank">Download Error File</a><button id="btnCloseDetail" class="ghost">Close</button></div></div>`;
    modal.style.position='fixed'; modal.style.left='50%'; modal.style.top='50%'; modal.style.transform='translate(-50%,-50%)'; modal.style.zIndex='1000'; modal.style.width='80%';
    document.body.appendChild(modal);
    modal.querySelector('#btnCloseDetail').addEventListener('click', ()=>modal.remove());
  }catch(e){ toast('Failed to load job details: '+e.message,false); }
}

window.addEventListener('DOMContentLoaded', ()=>{ renderJobs(); });
