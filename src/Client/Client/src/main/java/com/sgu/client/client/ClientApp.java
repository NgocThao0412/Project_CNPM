package com.sgu.client.client;

import com.sgu.client.DTO.*;
import com.sgu.client.BUS.ClientBUS;
import com.sgu.client.BUS.ValidationBUS;
import com.sgu.client.controller.ClientController;
import com.sgu.client.config.ClientConfig;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Giao dien Java Swing cho Client: Quan tri + POI + Noi dung + Khach tham quan. */
public final class ClientApp extends JFrame {
    private final ClientConfig config;
    private final ClientController controller;
    private AdminDTO admin;
    private final AudioPlayer player=new AudioPlayer();
    private final CardLayout cards=new CardLayout();
    private final JPanel root=new JPanel(cards);
    private final JLabel status=new JLabel("Sẵn sàng");
    private final JComboBox<String> mode=new JComboBox<>(new String[]{"Demo (không cần Backend)","Microservices Java (API thật)"});
    private final JTextField username=new JTextField("admin",20);
    private final JPasswordField password=new JPasswordField(20);
    private final DefaultTableModel poiModel=tableModel("ID","Mã POI","Tên POI","Khu vực","Vĩ độ","Kinh độ","Hiển thị");
    private final DefaultTableModel narrationModel=tableModel("ID","POI","Ngôn ngữ","Tiêu đề","Có audio","Hiển thị");
    private final JTable poiTable=new JTable(poiModel),narrationTable=new JTable(narrationModel);
    private final JTextField poiFilter=new JTextField();
    private final JComboBox<POIDTO> narrationPoi=new JComboBox<>(),visitorPoi=new JComboBox<>();
    private final JComboBox<String> narrationLang=new JComboBox<>(new String[]{"vi","en","fr"});
    private final JComboBox<String> visitorLang=new JComboBox<>(new String[]{"vi","en","fr"});
    private final JTextField narrationTitle=new JTextField();
    private final JTextArea narrationText=new JTextArea(6,40),visitorText=new JTextArea(7,28);
    private final JTextField gpsLat=new JTextField("10.75995",10),gpsLon=new JTextField("106.68205",10);
    private final JLabel visitorState=new JLabel("Chọn POI trên bản đồ để mô phỏng vị trí.");
    private final MapPanel map=new MapPanel();
    private List<POIDTO> poiCache=List.of(),poiShown=List.of();
    private List<NarrationDTO> narrationCache=List.of();
    private boolean adjusting=false;
    private long editingNarrationId=0;

    public ClientApp(ClientConfig config){
        super("Client — SGU Audio Guide | Java Microservices");this.config=config;controller=new ClientController(config);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setMinimumSize(new Dimension(1000,690));setSize(1220,780);
        setLocationRelativeTo(null);setLayout(new BorderLayout());
        setUIFont();
        add(root,BorderLayout.CENTER);
        root.add(createHome(),"home");root.add(createLogin(),"login");root.add(createAdmin(),"admin");
        status.setBorder(BorderFactory.createEmptyBorder(7,14,7,14));status.setOpaque(true);status.setBackground(new Color(238,244,241));
        add(status,BorderLayout.SOUTH);cards.show(root,"home");
        mode.addActionListener(e->{if(mode.getSelectedIndex()==0){controller.setDemo(true);setStatus("Chế độ demo: dữ liệu lưu trong RAM, KHÔNG phải MySQL.");}
                                 else{controller.setDemo(false);setStatus("Chế độ API thật: cần Java Backend và MySQL.");}
                                 admin=null;refreshVisitor();});
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){map.shutdown();player.stop();}});
        refreshVisitor();
    }
    private static void setUIFont(){
        Font f=new Font("Dialog",Font.PLAIN,14);
        Enumeration<Object> keys=UIManager.getDefaults().keys();
        while(keys.hasMoreElements()){Object k=keys.nextElement();Object value=UIManager.get(k);if(value instanceof javax.swing.plaf.FontUIResource)UIManager.put(k,new javax.swing.plaf.FontUIResource(f));}
    }
    private static JPanel panel(LayoutManager lm){JPanel p=new JPanel(lm);p.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));return p;}
    private static JLabel title(String s){JLabel l=new JLabel(s);l.setFont(new Font("Dialog",Font.BOLD,23));l.setForeground(new Color(20,80,58));return l;}
    private static JButton button(String text, Runnable action){JButton b=new JButton(text);b.addActionListener(e->action.run());return b;}
    private static DefaultTableModel tableModel(String... cols){return new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int row,int col){return false;}};}
    private void setStatus(String s){status.setText(s);}
    private void error(Throwable ex){Throwable actual=ex instanceof java.util.concurrent.ExecutionException&&ex.getCause()!=null?ex.getCause():ex;
        JOptionPane.showMessageDialog(this,actual.getMessage(),"Thông báo",JOptionPane.WARNING_MESSAGE);
        setStatus("Lỗi: "+actual.getMessage());}
    private <T> void job(String what,Callable<T> work,Consumer<T> done){
        setStatus(what+"...");
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground()throws Exception{return work.call();}
            @Override protected void done(){try {T result=get();done.accept(result);setStatus(what+" — hoàn tất");}catch(Exception e){error(e);}}
        }.execute();
    }
    private JPanel heading(String txt){JPanel p=panel(new BorderLayout(15,8));p.add(title(txt),BorderLayout.WEST);return p;}
    private JPanel createHome(){
        JPanel container=panel(new BorderLayout(10,12));
        JPanel toolbar=new JPanel(new BorderLayout(8,8));
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,10,2));left.add(title("SGU · Audio Guide"));
        JLabel hint=new JLabel("Client — Java 21 Swing");hint.setForeground(new Color(85,96,91));left.add(hint);
        toolbar.add(left,BorderLayout.WEST);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,2));right.add(mode);
        right.add(button("Đăng nhập quản trị",()->cards.show(root,"login")));
        toolbar.add(right,BorderLayout.EAST);container.add(toolbar,BorderLayout.NORTH);
        JPanel center=panel(new BorderLayout(10,12));
        JPanel actions=new JPanel(new BorderLayout(6,8));
        JPanel top=new JPanel(new GridLayout(2,1,5,6));
        top.add(title("Bản đồ và thuyết minh tham quan"));
        top.add(new JLabel("Không cần đăng nhập · Chọn POI trực tiếp hoặc nhập tọa độ để mô phỏng GPS"));
        actions.add(top,BorderLayout.NORTH);
        JPanel sidebar=panel(new BorderLayout(8,8));sidebar.setPreferredSize(new Dimension(335,600));
        JPanel controls=new JPanel();controls.setLayout(new BoxLayout(controls,BoxLayout.Y_AXIS));
        controls.add(new JLabel("ĐIỂM THAM QUAN (POI)"));controls.add(visitorPoi);
        controls.add(Box.createVerticalStrut(12));controls.add(new JLabel("NGÔN NGỮ"));controls.add(visitorLang);
        controls.add(Box.createVerticalStrut(8));
        controls.add(button("Chọn POI mô phỏng",()->{POIDTO p=(POIDTO)visitorPoi.getSelectedItem();if(p!=null){map.select(p);showVisitorContent();visitorState.setText("Đang mô phỏng tại POI: "+p.name());}}));
        controls.add(Box.createVerticalStrut(12));
        controls.add(new JLabel("TỌA ĐỘ GPS (NHẬP THỦ CÔNG)"));
        JPanel coordinates=new JPanel(new GridLayout(1,2,8,2));coordinates.add(gpsLat);coordinates.add(gpsLon);controls.add(coordinates);
        controls.add(button("Tìm POI gần tọa độ",this::locatePoi));
        controls.add(Box.createVerticalStrut(10));
        controls.add(visitorState);visitorState.setForeground(new Color(40,92,75));
        controls.add(Box.createVerticalStrut(8));controls.add(new JLabel("NỘI DUNG THUYẾT MINH"));
        visitorText.setEditable(false);visitorText.setLineWrap(true);visitorText.setWrapStyleWord(true);
        controls.add(new JScrollPane(visitorText));
        JPanel audioActions=new JPanel(new GridLayout(1,2,6,2));
        audioActions.add(button("▶ Phát audio",this::playVisitorAudio));
        audioActions.add(button("■ Dừng WAV",player::stop));controls.add(audioActions);
        controls.add(Box.createVerticalStrut(8));
        controls.add(button("Làm mới dữ liệu API",this::refreshVisitor));
        sidebar.add(controls,BorderLayout.CENTER);
        center.add(sidebar,BorderLayout.WEST);
        JPanel mapArea=panel(new BorderLayout(8,7));
        JPanel mapControls=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        mapControls.add(button("−",()->map.setZoom(map.getZoom()-1)));
        mapControls.add(button("+",()->map.setZoom(map.getZoom()+1)));
        mapArea.add(mapControls,BorderLayout.NORTH);mapArea.add(map,BorderLayout.CENTER);
        center.add(mapArea,BorderLayout.CENTER);
        map.setOnSelect(p->{visitorPoi.setSelectedItem(p);showVisitorContent();visitorState.setText("Vị trí mô phỏng: "+p.name());});
        visitorPoi.addActionListener(e->{if(!adjusting){POIDTO p=(POIDTO)visitorPoi.getSelectedItem();if(p!=null)map.select(p);showVisitorContent();}});
        visitorLang.addActionListener(e->showVisitorContent());
        actions.add(center,BorderLayout.CENTER);container.add(actions,BorderLayout.CENTER);
        return container;
    }
    private JPanel createLogin(){
        JPanel page=panel(new GridBagLayout());JPanel form=panel(new GridBagLayout());
        form.setBackground(new Color(242,248,244));form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207,222,211)),BorderFactory.createEmptyBorder(25,30,25,30)));
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(6,6,6,6);c.fill=GridBagConstraints.HORIZONTAL;c.gridx=0;c.gridwidth=2;
        c.gridy=0;form.add(title("Đăng nhập quản trị"),c);
        c.gridy++;form.add(new JLabel("Java HttpClient dùng phiên JSESSIONID (không tự tạo JWT)"),c);
        c.gridy++;form.add(new JLabel("Tên đăng nhập (username)"),c);
        c.gridy++;form.add(username,c);
        c.gridy++;form.add(new JLabel("Mật khẩu"),c);
        c.gridy++;form.add(password,c);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(button("← Trang khách",()->cards.show(root,"home")));
        buttons.add(button("Đăng nhập",this::login));
        c.gridy++;form.add(buttons,c);
        JLabel demo=new JLabel("Demo: admin@sgu.edu.vn / 123456. Backend thật: dùng tài khoản MySQL.");
        demo.setForeground(new Color(95,103,97));c.gridy++;form.add(demo,c);
        page.add(form);return page;
    }
    private JPanel createAdmin(){
        JPanel page=panel(new BorderLayout(8,8));
        JPanel top=heading("Quản trị nội dung SGU");
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(button("Xem trang khách",()->{refreshVisitor();cards.show(root,"home");}));
        right.add(button("Đăng xuất",this::logout));top.add(right,BorderLayout.EAST);page.add(top,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();tabs.addTab("Quản lý POI",createPoiTab());tabs.addTab("Quản lý nội dung / Audio",createNarrationTab());
        tabs.addChangeListener(e->{if(tabs.getSelectedIndex()==1)refreshNarrations();});
        page.add(tabs,BorderLayout.CENTER);return page;
    }
    private JPanel createPoiTab(){
        JPanel p=panel(new BorderLayout(8,10));
        JPanel tools=new JPanel(new BorderLayout(8,4));
        JPanel filter=new JPanel(new BorderLayout(6,3));filter.add(new JLabel("Tìm kiếm"),BorderLayout.WEST);filter.add(poiFilter,BorderLayout.CENTER);
        tools.add(filter,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(button("Làm mới",this::refreshPoi));actions.add(button("+ Thêm POI",()->editPoi(null)));
        actions.add(button("Sửa POI",()->{POIDTO x=selectedPoi();if(x!=null)editPoi(x);}));
        actions.add(button("Ẩn / Hiện",this::togglePoi));tools.add(actions,BorderLayout.EAST);
        p.add(tools,BorderLayout.NORTH);poiTable.setRowHeight(32);p.add(new JScrollPane(poiTable),BorderLayout.CENTER);
        JLabel note=new JLabel("Lưu ý: Backend hiện chỉ có GET /api/pois (POI đang hiển thị). Ghi dữ liệu thật cần API POST/PUT/PATCH.");
        note.setForeground(new Color(152,98,28));p.add(note,BorderLayout.SOUTH);
        poiFilter.getDocument().addDocumentListener(listener(this::filterPoi));return p;
    }
    private JPanel createNarrationTab(){
        JPanel p=panel(new BorderLayout(10,10));
        JPanel left=panel(new BorderLayout(8,6));left.setPreferredSize(new Dimension(510,500));
        JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(button("Làm mới nội dung",this::refreshNarrations));
        filters.add(button("Nội dung mới",()->loadNarrationForm(null)));left.add(filters,BorderLayout.NORTH);
        narrationTable.setRowHeight(29);
        narrationTable.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())loadNarrationForm(selectedNarration());});
        left.add(new JScrollPane(narrationTable),BorderLayout.CENTER);
        JPanel form=panel(new BorderLayout(8,8));
        JPanel fields=new JPanel(new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(5,4,5,4);c.gridx=0;c.gridy=0;c.weightx=0;
        fields.add(new JLabel("POI"),c);c.gridx=1;c.weightx=1;fields.add(narrationPoi,c);
        c.gridy++;c.gridx=0;c.weightx=0;fields.add(new JLabel("Ngôn ngữ"),c);c.gridx=1;c.weightx=1;fields.add(narrationLang,c);
        c.gridy++;c.gridx=0;c.weightx=0;fields.add(new JLabel("Tiêu đề"),c);c.gridx=1;c.weightx=1;fields.add(narrationTitle,c);
        form.add(fields,BorderLayout.NORTH);
        narrationText.setLineWrap(true);narrationText.setWrapStyleWord(true);
        form.add(new JScrollPane(narrationText),BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(button("Lưu nội dung",this::saveNarration));
        actions.add(button("Ẩn / Hiện",this::toggleNarration));
        actions.add(button("Tải audio MP3",this::uploadAudio));
        form.add(actions,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,form);split.setResizeWeight(.48);
        p.add(split,BorderLayout.CENTER);
        JLabel note=new JLabel("Nếu chưa có NarrationDTO/Audio service, API thật sẽ báo lỗi rõ ràng. Demo cho phép nhập và nghe file cục bộ.");
        note.setForeground(new Color(152,98,28));p.add(note,BorderLayout.SOUTH);
        return p;
    }
    private static DocumentListener listener(Runnable work){return new DocumentListener(){
        public void insertUpdate(DocumentEvent e){work.run();}
        public void removeUpdate(DocumentEvent e){work.run();}
        public void changedUpdate(DocumentEvent e){work.run();}
    };}
    private void login(){
        String user=username.getText().trim(),secret=new String(password.getPassword());
        if(user.isEmpty()||secret.isEmpty()){error(new IllegalArgumentException("Vui lòng nhập tài khoản và mật khẩu."));return;}
        job("Đang xác thực qua Auth API",()->controller.login(user,secret),result->{admin=result;password.setText("");cards.show(root,"admin");
            refreshPoi();refreshNarrations();});
    }
    private void logout(){job("Đăng xuất",()->{controller.logout();return null;},x->{admin=null;player.stop();cards.show(root,"login");});}
    private void refreshPoi(){job("Tải danh sách POI",controller::pois,list->{poiCache=list;filterPoi();updatePoiCombos();});}
    private void filterPoi(){String query=poiFilter.getText().trim().toLowerCase(Locale.ROOT);
        poiShown=poiCache.stream().filter(x->x.code().toLowerCase(Locale.ROOT).contains(query)||x.name().toLowerCase(Locale.ROOT).contains(query)||x.area().toLowerCase(Locale.ROOT).contains(query)).toList();
        poiModel.setRowCount(0);for(POIDTO x:poiShown)poiModel.addRow(new Object[]{x.id(),x.code(),x.name(),x.area(),x.latitude(),x.longitude(),x.visible()?"Hiện":"Ẩn"});
    }
    private POIDTO selectedPoi(){int row=poiTable.getSelectedRow();if(row<0||row>=poiShown.size()){error(new IllegalArgumentException("Vui lòng chọn POI trong bảng."));return null;}return poiShown.get(row);}
    private void editPoi(POIDTO p){
        JTextField code=new JTextField(p==null?"":p.code());JTextField name=new JTextField(p==null?"":p.name());
        JTextField areaId=new JTextField(p==null?"":p.areaId()==0?"":String.valueOf(p.areaId()));
        JTextField area=new JTextField(p==null?"":p.area());
        JTextField lat=new JTextField(p==null?"10.76012":String.valueOf(p.latitude()));
        JTextField lon=new JTextField(p==null?"106.68162":String.valueOf(p.longitude()));
        JTextField radius=new JTextField(p==null?"45":String.valueOf(p.radiusM()));
        JTextField image=new JTextField(p==null?"":p.imageUrl());
        JTextArea desc=new JTextArea(p==null?"":p.description(),3,35);desc.setLineWrap(true);
        JCheckBox visible=new JCheckBox("Hiển thị với khách tham quan",p==null||p.visible());
        JPanel fields=new JPanel(new GridLayout(0,2,8,8));
        String[] labels={"Mã POI","Tên POI","Area ID (Backend)","Khu vực (UI)","Vĩ độ","Kinh độ","Bán kính GPS (m)","URL ảnh"};
        JTextField[] values={code,name,areaId,area,lat,lon,radius,image};for(int i=0;i<labels.length;i++){fields.add(new JLabel(labels[i]));fields.add(values[i]);}
        JPanel content=panel(new BorderLayout(10,10));content.add(fields,BorderLayout.NORTH);
        JPanel bottom=panel(new BorderLayout());bottom.add(new JLabel("Mô tả"),BorderLayout.NORTH);bottom.add(new JScrollPane(desc),BorderLayout.CENTER);bottom.add(visible,BorderLayout.SOUTH);content.add(bottom,BorderLayout.CENTER);
        if(JOptionPane.showConfirmDialog(this,content,p==null?"Thêm POI":"Sửa POI",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        try {
            long id=p==null?0:p.id(),aid=areaId.getText().isBlank()?0:Long.parseLong(areaId.getText().trim());
            POIDTO next=new POIDTO(id,code.getText().trim(),name.getText().trim(),aid,area.getText().trim(),desc.getText().trim(),
                    Double.parseDouble(lat.getText().trim()),Double.parseDouble(lon.getText().trim()),Double.parseDouble(radius.getText().trim()),image.getText().trim(),visible.isSelected());
            ValidationBUS.validatePoi(next);job("Lưu POI",()->controller.savePoi(next),x->refreshPoi());
        }catch(Exception e){error(new IllegalArgumentException("Dữ liệu POI không hợp lệ: "+e.getMessage()));}
    }
    private void togglePoi(){POIDTO p=selectedPoi();if(p==null)return;job("Đổi hiển thị POI",()->{controller.visiblePoi(p.id(),!p.visible());return null;},x->refreshPoi());}
    private void updatePoiCombos(){adjusting=true;POIDTO previous=(POIDTO)visitorPoi.getSelectedItem();
        visitorPoi.removeAllItems();narrationPoi.removeAllItems();
        for(POIDTO p:poiCache){narrationPoi.addItem(p);if(p.visible())visitorPoi.addItem(p);}
        if(previous!=null)for(int i=0;i<visitorPoi.getItemCount();i++)if(visitorPoi.getItemAt(i).id()==previous.id()){visitorPoi.setSelectedIndex(i);break;}
        adjusting=false;map.setPois(poiCache.stream().filter(POIDTO::visible).toList());showVisitorContent();
    }
    private void refreshNarrations(){job("Tải nội dung thuyết minh",controller::narrations,list->{narrationCache=list;renderNarrationTable();showVisitorContent();});}
    private void renderNarrationTable(){narrationModel.setRowCount(0);
        for(NarrationDTO n:narrationCache){String poiName=poiCache.stream().filter(p->p.id()==n.poiId()).findFirst().map(POIDTO::name).orElse("POI #"+n.poiId());
            narrationModel.addRow(new Object[]{n.id(),poiName,n.languageCode(),n.title(),n.audioUrl().isBlank()?"Chưa":"Có",n.visible()?"Hiện":"Ẩn"});}
    }
    private NarrationDTO selectedNarration(){int row=narrationTable.getSelectedRow();return row<0||row>=narrationCache.size()?null:narrationCache.get(row);}
    private void loadNarrationForm(NarrationDTO n){editingNarrationId=n==null?0:n.id();
        narrationTitle.setText(n==null?"":n.title());narrationText.setText(n==null?"":n.content());
        if(n!=null){narrationLang.setSelectedItem(n.languageCode());for(int i=0;i<narrationPoi.getItemCount();i++)if(narrationPoi.getItemAt(i).id()==n.poiId()){narrationPoi.setSelectedIndex(i);break;}}
        else if(narrationPoi.getItemCount()>0)narrationPoi.setSelectedIndex(0);
    }
    private void saveNarration(){POIDTO p=(POIDTO)narrationPoi.getSelectedItem();if(p==null){error(new IllegalArgumentException("Chưa có POI."));return;}
        NarrationDTO previous=narrationCache.stream().filter(n->n.id()==editingNarrationId).findFirst().orElse(null);
        NarrationDTO n=new NarrationDTO(editingNarrationId,p.id(),(String)narrationLang.getSelectedItem(),
                narrationTitle.getText().trim(),narrationText.getText().trim(),previous==null?"":previous.audioUrl(),previous==null||previous.visible());
        try{ValidationBUS.validateNarration(n);}catch(Exception e){error(e);return;}
        job("Lưu thuyết minh",()->controller.saveNarration(n),saved->{editingNarrationId=saved.id();refreshNarrations();});
    }
    private void toggleNarration(){NarrationDTO n=selectedNarration();if(n==null){error(new IllegalArgumentException("Chọn nội dung trong bảng."));return;}
        job("Ẩn / Hiện thuyết minh",()->{controller.visibleNarration(n.id(),!n.visible());return null;},x->refreshNarrations());}
    private void uploadAudio(){NarrationDTO n=selectedNarration();if(n==null){error(new IllegalArgumentException("Chọn một nội dung đã lưu để tải audio."));return;}
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Chọn tệp MP3 để tải lên");
        if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){Path file=chooser.getSelectedFile().toPath();
            double duration=0;
            if(!controller.isDemo()){
                String raw=JOptionPane.showInputDialog(this,"Thời lượng MP3 chính xác (giây):","60");
                if(raw==null)return;
                try{duration=Double.parseDouble(raw.trim());if(!Double.isFinite(duration)||duration<=0)throw new NumberFormatException();}
                catch(NumberFormatException ex){error(new IllegalArgumentException("Nhập thời lượng MP3 dương, ví dụ 35.5"));return;}
            }
            final double d=duration;
            job("Tải audio",()->{controller.uploadAudio(n.id(),file,d);return null;},x->refreshNarrations());}
    }
    private void refreshVisitor(){job("Tải POI cho khách tham quan",controller::pois,list->{poiCache=list;updatePoiCombos();
        if(controller.isDemo()){try{narrationCache=controller.narrations();}catch(Exception ignored){}showVisitorContent();}
        else{narrationCache=List.of();if(config.enabled("narration.read"))job("Tải nội dung",controller::narrations,ns->{narrationCache=ns;showVisitorContent();});
            else visitorState.setText("Backend hiện CHƯA có NarrationDTO service — không có audio thật.");}
    });}
    private void showVisitorContent(){POIDTO p=(POIDTO)visitorPoi.getSelectedItem();String lang=(String)visitorLang.getSelectedItem();
        if(p==null){visitorText.setText("Chưa có POI hiển thị.");return;}
        NarrationDTO n=ClientBUS.findNarration(narrationCache,p.id(),lang);visitorText.setText(n==null?"Chưa có nội dung thuyết minh bằng "+lang+" cho "+p.name()+".\n"+p.description():n.title()+"\n\n"+n.content());
    }
    private void playVisitorAudio(){POIDTO p=(POIDTO)visitorPoi.getSelectedItem();if(p==null){error(new IllegalArgumentException("Hãy chọn POI."));return;}
        String lang=(String)visitorLang.getSelectedItem();NarrationDTO n=ClientBUS.findNarration(narrationCache,p.id(),lang);
        if(n==null){error(new IllegalArgumentException("POI chưa có nội dung ngôn ngữ "+lang+"."));return;}
        job("Phát audio",()->player.play(n.audioUrl()),message->{visitorState.setText(message);
            if(n.audioUrl()!=null&&!n.audioUrl().isBlank()){
                // Chỉ ghi log trong API thật khi được cấu hình đầy đủ.
                if(controller.isDemo())return;
                if(config.enabled("visitor.events"))job("Ghi nhận lượt nghe",()->{controller.listenEvent(p.id(),lang,true);return null;},x->{});
            }});
    }
    private void locatePoi(){
        try {
            double lat=Double.parseDouble(gpsLat.getText().trim());
            double lon=Double.parseDouble(gpsLon.getText().trim());
            POIDTO closest=ClientBUS.nearestPoi(poiCache,lat,lon);
            if(closest==null){
                visitorState.setText("Không có POI đang hiển thị trong bán kính GPS đã cấu hình.");
            }else{
                double distance=ClientBUS.haversine(lat,lon,closest.latitude(),closest.longitude());
                visitorPoi.setSelectedItem(closest);map.select(closest);
                visitorState.setText(String.format(Locale.US,"GPS mô phỏng: %s (%.0fm)",closest.name(),distance));
            }
        }catch(Exception e){error(new IllegalArgumentException("Vĩ độ / kinh độ không hợp lệ."));}
    }
    public static void main(String[] args){
        Path configPath=Path.of(args.length>0?args[0]:"config.properties");
        try{ClientConfig config=new ClientConfig(configPath);SwingUtilities.invokeLater(()->new ClientApp(config).setVisible(true));}
        catch(Exception ex){System.err.println("Không đọc được config: "+ex.getMessage());System.exit(1);}
    }
}
