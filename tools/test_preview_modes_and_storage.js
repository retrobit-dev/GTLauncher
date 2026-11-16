const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {JSDOM} = require('jsdom');
const root = path.join(__dirname, '../preview');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const key = 'gtlauncher.worlds.v1';
function setup(value) {
  const dom = new JSDOM(html, {url: 'https://preview.example/', runScripts: 'outside-only'});
  const w = dom.window, d = w.document;
  if (value !== undefined) w.localStorage.setItem(key, JSON.stringify(value));
  for (const dialog of d.querySelectorAll('dialog')) { dialog.showModal = () => dialog.open = true; dialog.close = () => dialog.open = false; }
  w.sceneEditing = {update(){}, hitTest(){return null}};
  for (const name of ['worlds.js','object-editor.js','preview-modes.js']) w.eval(fs.readFileSync(path.join(root,name),'utf8'));
  return {w,d};
}
const old = {id:'old',name:'Home',description:'',color:'#a8c795',showCharacter:true};
{
 const original={worlds:[old],activeId:'old'};const {w,d}=setup(original);
 assert.deepEqual(JSON.parse(w.localStorage.getItem(key)),original,'opening preview must not write or migrate Home');
 d.getElementById('mode-map-editor').click();
 assert.equal(d.getElementById('mode-map-editor').getAttribute('aria-pressed'),'true');
 assert.equal(d.getElementById('object-edit-button').getAttribute('aria-pressed'),'true');
 d.getElementById('mode-gameplay').click();assert.equal(d.getElementById('object-edit-button').getAttribute('aria-pressed'),'false');
 assert.equal(d.getElementById('mode-gameplay').getAttribute('aria-pressed'),'true');
 w.close();
}
{
 const value={worlds:[{...old, roads:[{column:1,row:2}]}],activeId:'old'};const {w,d}=setup(value);
 assert.equal(d.getElementById('world-backup').hidden,false,'unsupported fields provide backup');
 d.getElementById('world-new').click();d.getElementById('world-name').value='Another';
 d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
 assert.deepEqual(JSON.parse(w.localStorage.getItem(key)),value,'unsupported newer world must never be overwritten');w.close();
}
{
 const value={worlds:[old],activeId:'old',future:'extra'};const {w,d}=setup(value);
 assert.equal(d.getElementById('world-backup').hidden,false);
 assert.deepEqual(JSON.parse(w.localStorage.getItem(key)),value);w.close();
}
{
 const value={worlds:[old],activeId:'old'};const {w,d}=setup(value);
 const newer={worlds:[{...old,roads:[]}],activeId:'old'};w.localStorage.setItem(key,JSON.stringify(newer));
 d.getElementById('world-new').click();d.getElementById('world-name').value='Another';
 d.getElementById('world-form').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
 assert.deepEqual(JSON.parse(w.localStorage.getItem(key)),newer,'concurrent data change must not be overwritten');w.close();
}
{
 const {w,d}=setup();d.getElementById('mode-map-editor').click();
 assert.equal(d.getElementById('edit-status').hidden,true,'Base World does not show an unsolicited warning');
 assert.equal(d.querySelector('.restore-notice'),null);
 assert.match(d.getElementById('world-feature-limits').textContent,/hanya tersedia di preview/);
 assert.match(d.getElementById('world-feature-limits').textContent,/Android masih belum tersedia/);
 assert.equal(d.getElementById('object-edit-button').getAttribute('aria-pressed'),'false');
 d.getElementById('mode-gameplay').click();assert.equal(d.getElementById('edit-status').hidden,true);w.close();
}
console.log('Mode tabs and icon controls; storage read-only guard, no automatic migration and concurrent write guard: OK');
