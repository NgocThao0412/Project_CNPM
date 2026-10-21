/* Dữ liệu/chức năng giả lập để chạy frontend mà KHÔNG cần backend. KHÔNG dùng để xác thực thật. */
const KEY = 'ny-client-demo-v1';
const delay = () => new Promise(resolve => setTimeout(resolve, 120));

const initialData = () => ({
  pois: [
    { id: 1, code: 'SGU-001', name: 'Cổng chính Đại học Sài Gòn', area: 'Khu trung tâm', description: 'Điểm đón tiếp và hướng dẫn khách tham quan.', latitude: 10.76012, longitude: 106.68162, radius_m: 45, image_url: '', is_visible: true },
    { id: 2, code: 'SGU-002', name: 'Thư viện', area: 'Khu học tập', description: 'Không gian đọc sách, tự học và khai thác học liệu.', latitude: 10.75980, longitude: 106.68218, radius_m: 45, image_url: '', is_visible: true },
    { id: 3, code: 'SGU-003', name: 'Giảng đường', area: 'Khu học tập', description: 'Khu vực phục vụ học tập và các hoạt động giảng dạy.', latitude: 10.75949, longitude: 106.68278, radius_m: 40, image_url: '', is_visible: true },
    { id: 4, code: 'SGU-004', name: 'Hội trường', area: 'Khu sinh hoạt', description: 'Không gian tổ chức chương trình, hội nghị và sinh hoạt chung.', latitude: 10.76048, longitude: 106.68276, radius_m: 50, image_url: '', is_visible: false },
  ],
  narrations: [
    { id: 1, poi_id: 1, language_code: 'vi', title: 'Chào mừng đến với Đại học Sài Gòn', body: 'Chào mừng bạn đến với Trường Đại học Sài Gòn. Đây là vị trí cổng chính, nơi bắt đầu hành trình khám phá khuôn viên trường.', is_visible: true, audio_url: '' },
    { id: 2, poi_id: 1, language_code: 'en', title: 'Welcome to Saigon University', body: 'Welcome to Saigon University. This is the main entrance, where your campus discovery begins.', is_visible: true, audio_url: '' },
    { id: 3, poi_id: 2, language_code: 'vi', title: 'Giới thiệu thư viện', body: 'Thư viện là nơi sinh viên tìm kiếm tài liệu và sử dụng không gian tự học.', is_visible: true, audio_url: '' },
    { id: 4, poi_id: 2, language_code: 'en', title: 'Library introduction', body: 'The library provides learning materials and quiet spaces for independent study.', is_visible: true, audio_url: '' },
    { id: 5, poi_id: 3, language_code: 'vi', title: 'Giới thiệu giảng đường', body: 'Khu giảng đường phục vụ các hoạt động học tập, giảng dạy và trao đổi kiến thức.', is_visible: true, audio_url: '' },
  ],
  languages: [ { code: 'vi', name: 'Tiếng Việt' }, { code: 'en', name: 'English' }, { code: 'fr', name: 'Français' } ],
  routes: [ { id: 1, name: 'Khám phá SGU', description: 'Lộ trình tham quan gợi ý', poi_ids: [1, 2, 3] } ],
  logs: [], sessions: [],
});

function read() {
  try { return JSON.parse(localStorage.getItem(KEY)) || initialData(); }
  catch { return initialData(); }
}
function write(data) {
  try {
    // Blob URL chỉ tồn tại trong phiên trình duyệt hiện tại; không lưu chúng vào localStorage.
    const copy = JSON.parse(JSON.stringify(data));
    for (const n of copy.narrations) if (n.audio_url?.startsWith('blob:')) n.audio_url = '';
    localStorage.setItem(KEY, JSON.stringify(copy));
  } catch { /* Quota hạn chế: dữ liệu vẫn tồn tại trong bộ nhớ của trang. */ }
}
let db = read();
const clone = x => JSON.parse(JSON.stringify(x));
const nextId = collection => 1 + Math.max(0, ...collection.map(x => Number(x.id) || 0));

export const mock = {
  async login(email, password) {
    await delay();
    if (email.trim().toLowerCase() !== 'admin@sgu.edu.vn' || password !== '123456') {
      throw new Error('Tài khoản hoặc mật khẩu không đúng.');
    }
    return { token: 'demo-only-token', user: { name: 'Quản trị viên Demo', email: 'admin@sgu.edu.vn' } };
  },
  async logout() { await delay(); },
  async pois(publicOnly = false) { await delay(); return clone(db.pois.filter(p => !publicOnly || p.is_visible)); },
  async narrations(publicOnly = false) { await delay(); return clone(db.narrations.filter(n => !publicOnly || (n.is_visible && db.pois.some(p => p.id === n.poi_id && p.is_visible)))); },
  async languages() { return clone(db.languages); },
  async routes() { return clone(db.routes); },
  async savePoi(item) {
    await delay();
    if (!item.name?.trim() || !item.code?.trim()) throw new Error('Mã và tên POI không được để trống.');
    if (db.pois.some(p => p.code.toLowerCase() === item.code.toLowerCase() && Number(p.id) !== Number(item.id))) throw new Error('Mã POI đã tồn tại.');
    if (!Number.isFinite(item.latitude) || Math.abs(item.latitude) > 90 || !Number.isFinite(item.longitude) || Math.abs(item.longitude) > 180 || !(item.radius_m > 0)) throw new Error('Tọa độ GPS hoặc bán kính không hợp lệ.');
    let result;
    if (item.id) {
      const index = db.pois.findIndex(p => p.id === Number(item.id));
      if (index === -1) throw new Error('POI không còn tồn tại.');
      result = { ...db.pois[index], ...item, id: Number(item.id) };
      db.pois[index] = result;
    } else {
      result = { ...item, id: nextId(db.pois) };
      db.pois.push(result);
    }
    write(db); return clone(result);
  },
  async togglePoi(id, visible) {
    const item = db.pois.find(p => p.id === Number(id));
    if (!item) throw new Error('Không tìm thấy POI.');
    item.is_visible = !!visible; write(db); await delay();
  },
  async saveNarration(item) {
    await delay();
    if (!db.pois.some(p => p.id === Number(item.poi_id))) throw new Error('POI không tồn tại.');
    if (!db.languages.some(l => l.code === item.language_code)) throw new Error('Ngôn ngữ không hợp lệ.');
    if (!item.title?.trim() || !item.body?.trim()) throw new Error('Vui lòng nhập tiêu đề và nội dung thuyết minh.');
    if (db.narrations.some(n => n.poi_id === Number(item.poi_id) && n.language_code === item.language_code && n.id !== Number(item.id))) throw new Error('POI đã có nội dung ở ngôn ngữ này.');
    let result;
    if (item.id) {
      const index = db.narrations.findIndex(n => n.id === Number(item.id));
      if (index < 0) throw new Error('Nội dung không còn tồn tại.');
      result = { ...db.narrations[index], ...item, id: Number(item.id) }; db.narrations[index] = result;
    } else {
      result = { ...item, id: nextId(db.narrations), audio_url: '' }; db.narrations.push(result);
    }
    write(db); return clone(result);
  },
  async toggleNarration(id, visible) {
    const n = db.narrations.find(x => x.id === Number(id));
    if (!n) throw new Error('Nội dung không còn tồn tại.');
    n.is_visible = !!visible; write(db); await delay();
  },
  async uploadAudio(id, file) {
    const n = db.narrations.find(x => x.id === Number(id));
    if (!n) throw new Error('Không tìm thấy nội dung thuyết minh.');
    if (!file || !(/\.(mp3|wav|m4a|ogg)$/i).test(file.name)) throw new Error('Chỉ hỗ trợ file âm thanh .mp3, .wav, .m4a, .ogg.');
    if (file.size > 20 * 1024 * 1024) throw new Error('File audio phải nhỏ hơn 20 MB.');
    if (n.audio_url?.startsWith('blob:')) URL.revokeObjectURL(n.audio_url);
    n.audio_url = URL.createObjectURL(file); write(db); await delay();
    return { audio_url: n.audio_url };
  },
  async createSession() { const id = crypto.randomUUID?.() || String(Date.now()); db.sessions.push({ id, at: new Date().toISOString() }); write(db); return { id }; },
  async logListen(payload) { db.logs.push({ ...payload, started_at: new Date().toISOString() }); write(db); },
  reset() { db = initialData(); write(db); },
};
