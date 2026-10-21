/* Chạy: node --test tests/mock.test.mjs */
import test from 'node:test';
import assert from 'node:assert/strict';

globalThis.localStorage = {
  data: new Map(),
  getItem(k) { return this.data.get(k) ?? null; },
  setItem(k, v) { this.data.set(k, String(v)); },
  removeItem(k) { this.data.delete(k); },
};
const { mock } = await import('../src/mock.js');

test('đăng nhập: mật khẩu sai bị từ chối, đúng nhận phiên demo', async () => {
  await assert.rejects(() => mock.login('admin@sgu.edu.vn', 'invalid'));
  assert.ok((await mock.login('admin@sgu.edu.vn', '123456')).token);
});

test('POI: phân biệt công khai, thêm, ẩn, kiểm tra GPS và mã trùng', async () => {
  mock.reset();
  assert.equal((await mock.pois(false)).length, 4);
  assert.equal((await mock.pois(true)).length, 3);
  const item = { code: 'SGU-T', name: 'POI test', area: '', description: '', latitude: 10.76, longitude: 106.68, radius_m: 50, is_visible: true, image_url: '' };
  const added = await mock.savePoi(item);
  assert.equal((await mock.pois()).length, 5);
  await assert.rejects(() => mock.savePoi({ ...item, name: 'Duplicate' }));
  await assert.rejects(() => mock.savePoi({ ...item, code: 'SGU-INVALID', latitude: 99 }));
  await mock.togglePoi(added.id, false);
  assert.equal((await mock.pois(true)).some(p => p.id === added.id), false);
});

test('Nội dung: CRUD cơ bản, ràng buộc POI-ngôn ngữ, ẩn/hiện', async () => {
  mock.reset();
  const n = await mock.saveNarration({ poi_id: 3, language_code: 'en', title: 'Building', body: 'A building.', is_visible: true });
  assert.ok(n.id);
  await assert.rejects(() => mock.saveNarration({ poi_id: 3, language_code: 'en', title: 'Duplicate', body: 'Duplicate.', is_visible: true }));
  await mock.toggleNarration(n.id, false);
  assert.equal((await mock.narrations()).find(x => x.id === n.id).is_visible, false);
});

test('Khởi tạo phiên tham quan ẩn danh và ghi nhận lượt nghe', async () => {
  const session = await mock.createSession();
  assert.ok(session.id);
  await mock.logListen({ session_id: session.id, poi_id: 1, language_code: 'vi', source: 'simulation' });
});
