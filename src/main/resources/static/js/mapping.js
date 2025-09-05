/*
Drag & Drop Mapping Designer
- Left column: DB columns (drop targets)
- Right column: File headers (draggable items)
- Drag a header onto a DB column to map it.
*/
async function renderMappingDesigner(){
  const host = document.getElementById('tab-mapping') || document.body;
  host.innerHTML = '';
  const card = document.createElement('div'); card.className='card';
  card.innerHTML = `<h3>Mapping Designer (Drag & Drop)</h3>
  <div class="grid">
    <div class="col-4" id="projBox"></div>
    <div class="col-4"><label>Schema</label><input id="mapSchema" placeholder="public" /></div>
    <div class="col-4"><label>Table</label><input id="mapTable" placeholder="orders" /></div>
    <div class="col-12"><label>Sample file header (comma separated) or paste header row</label><input id="sampleHeader" placeholder="id,name,amount,date" /></div>
    <div class="col-12"><button id="btnFetchCols" class="primary">Fetch DB Columns</button></div>
  </div>
  <div id="mapArea" style="display:flex;gap:16px;margin-top:12px"></div>
  <div class="row" style="margin-top:12px"><button id="btnSaveMap" class="primary">Save Mapping</button></div>`;
  host.appendChild(card);
  const projSel = projectSelect('mapProjectSel'); card.querySelector('#projBox').appendChild(projSel); await fillProjectSelect(projSel.querySelector('select'));

  card.querySelector('#btnFetchCols').addEventListener('click', async ()=>{
    const projectId = Store.projectId; const schema = card.querySelector('#mapSchema').value||'public'; const table = card.querySelector('#mapTable').value;
    if(!projectId || !table){ toast('Select project and enter table', false); return; }
    try{
      const cols = await api(`/api/dbmeta/projects/${projectId}/tables/${encodeURIComponent(schema)}/${encodeURIComponent(table)}/columns`,'GET');
      const headers = (card.querySelector('#sampleHeader').value||'').split(',').map(s=>s.trim()).filter(Boolean);
      renderDnD(cols, headers);
    }catch(e){ toast('Failed to fetch columns: '+e.message,false); }
  });

  card.querySelector('#btnSaveMap').addEventListener('click', async ()=>{
    const mappings = [];
    document.querySelectorAll('.db-col').forEach(el=>{
      const target = el.dataset.col;
      const src = el.dataset.mapped || null;
      const req = el.querySelector('.req').checked;
      const def = el.querySelector('.def').value || null;
      const trans = el.querySelector('.trans').value || null;
      if(src) mappings.push({ target: target, source: src, required: req, defaultValue: def, transform: trans });
    });
    const mappingJson = { columns: mappings, upsertKeys: mappings.filter(m=>m.required).map(m=>m.target) };
    const payload = { projectId: Store.projectId, filePattern: '.*', mappingJson: JSON.stringify(mappingJson) };
    try{ const res = await api('/api/mappings','POST', payload); toast('Saved mapping '+res.id, true); }catch(e){ toast('Save failed: '+e.message,false); }
  });

  function renderDnD(cols, headers){
    const area = document.getElementById('mapArea'); area.innerHTML='';
    const left = document.createElement('div'); left.style.flex='1'; left.innerHTML = '<h4>DB Columns</h4>';
    const right = document.createElement('div'); right.style.flex='1'; right.innerHTML = '<h4>File Headers (drag these)</h4>';
    const list = document.createElement('div'); list.style.display='grid'; list.style.gap='8px';
    cols.forEach(c=>{
      const div = document.createElement('div'); div.className='db-col'; div.dataset.col = c.name; div.draggable = false;
      div.style.border='1px solid #ddd'; div.style.padding='8px'; div.style.borderRadius='6px';
      div.innerHTML = `<div style="font-weight:600">${c.name}</div><div style="font-size:12px;color:#666">${c.type}</div>
      <div style="margin-top:6px">Mapped: <span class="mappedVal">-</span></div>
      <div style="margin-top:6px"><label>Required <input type="checkbox" class="req" /></label> Default: <input class="def" /></div>
      <div style="margin-top:6px">Transform: <input class="trans" placeholder="int,date,boolean" /></div>`;
      // allow drop
      div.addEventListener('dragover', e=>e.preventDefault());
      div.addEventListener('drop', e=>{
        e.preventDefault();
        const hdr = e.dataTransfer.getData('text/plain');
        div.dataset.mapped = hdr;
        div.querySelector('.mappedVal').textContent = hdr;
      });
      list.appendChild(div);
    });
    const hdrBox = document.createElement('div'); hdrBox.style.display='grid'; hdrBox.style.gap='6px';
    headers.forEach(h=>{
      const el = document.createElement('div'); el.className='hdr-item'; el.draggable = true; el.textContent = h;
      el.style.padding='6px'; el.style.border='1px solid #ccc'; el.style.borderRadius='6px'; el.style.cursor='grab';
      el.addEventListener('dragstart', e=> e.dataTransfer.setData('text/plain', h));
      hdrBox.appendChild(el);
    });
    left.appendChild(list); right.appendChild(hdrBox);
    area.appendChild(left); area.appendChild(right);
  }
}

window.addEventListener('DOMContentLoaded', ()=>{ if(document.getElementById('tab-mapping')) renderMappingDesigner(); });
