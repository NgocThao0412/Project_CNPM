package com.sgu.client.client;

import com.sgu.client.DTO.POIDTO;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Bản đồ OpenStreetMap raster tiles hiển thị và tương tác 100% bằng Java Swing.
 * Nếu offline, chỉ hiển thị lưới tọa độ + markers, không ngụy tạo nền bản đồ.
 */
public final class MapPanel extends JPanel {
    private final Map<String,BufferedImage> tiles=new ConcurrentHashMap<>();
    private final Set<String> loading=ConcurrentHashMap.newKeySet();
    private final ExecutorService pool=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"client-map-tiles");t.setDaemon(true);return t;});
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private List<POIDTO> pois=List.of();
    private POIDTO selected;
    private double lat=10.75995,lon=106.68205;
    private int zoom=17;
    private Consumer<POIDTO> onSelect=p->{};
    private Point dragStart;
    private double dragLat,dragLon;
    public MapPanel(){
        setOpaque(true);setBackground(new Color(231,238,234));setPreferredSize(new Dimension(620,470));
        MouseAdapter mouse=new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){dragStart=e.getPoint();dragLat=lat;dragLon=lon;}
            @Override public void mouseDragged(MouseEvent e){if(dragStart==null)return;
                Point2D p=project(dragLat,dragLon,zoom);double nx=p.getX()+dragStart.x-e.getX();double ny=p.getY()+dragStart.y-e.getY();
                lat=reverseLat(ny,zoom);lon=nx/(256*Math.pow(2,zoom))*360-180;repaint();}
            @Override public void mouseReleased(MouseEvent e){if(dragStart!=null && dragStart.distance(e.getPoint())<5){
                for(POIDTO p:pois){Point2D x=screen(p);if(x.distance(e.getPoint())<17){selected=p;onSelect.accept(p);break;}}
                repaint();}dragStart=null;}
            @Override public void mouseWheelMoved(MouseWheelEvent e){setZoom(zoom-e.getWheelRotation());}
        };addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
    }
    public void setOnSelect(Consumer<POIDTO> c){onSelect=c;}
    public void setPois(List<POIDTO> p){pois=List.copyOf(p);if(!p.isEmpty()&&selected==null){selected=p.get(0);center(selected);}repaint();}
    public void select(POIDTO p){selected=p;if(p!=null)center(p);repaint();}
    public void center(POIDTO p){if(p!=null&&Math.abs(p.latitude())<=90&&Math.abs(p.longitude())<=180){lat=p.latitude();lon=p.longitude();repaint();}}
    public void setZoom(int z){zoom=Math.max(3,Math.min(19,z));repaint();}
    public int getZoom(){return zoom;}
    public static Point2D project(double lat,double lon,int z){
        double n=256*Math.pow(2,z),safe=Math.max(-85.0511,Math.min(85.0511,lat));double r=Math.toRadians(safe);
        return new Point2D.Double((lon+180)/360*n,(1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2*n);
    }
    private static double reverseLat(double y,int z){double n=Math.PI-2*Math.PI*y/(256*Math.pow(2,z));return Math.toDegrees(Math.atan(Math.sinh(n)));}
    private Point2D screen(POIDTO p){Point2D c=project(lat,lon,zoom),x=project(p.latitude(),p.longitude(),zoom);return new Point2D.Double(getWidth()/2.0+x.getX()-c.getX(),getHeight()/2.0+x.getY()-c.getY());}
    @Override protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D a=(Graphics2D)g.create();
        a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int w=getWidth(),h=getHeight();a.setColor(new Color(226,239,230));a.fillRect(0,0,w,h);
        a.setColor(new Color(205,219,210));for(int x=0;x<w;x+=44)a.drawLine(x,0,x,h);for(int y=0;y<h;y+=44)a.drawLine(0,y,w,y);
        Point2D center=project(lat,lon,zoom);
        int minx=(int)Math.floor((center.getX()-w/2.0)/256),maxx=(int)Math.floor((center.getX()+w/2.0)/256);
        int miny=(int)Math.floor((center.getY()-h/2.0)/256),maxy=(int)Math.floor((center.getY()+h/2.0)/256);
        int world=1<<zoom;
        for(int tx=minx;tx<=maxx;tx++)for(int ty=miny;ty<=maxy;ty++){
            if(ty<0||ty>=world)continue;
            int x=(int)Math.round(w/2.0+tx*256-center.getX()),y=(int)Math.round(h/2.0+ty*256-center.getY());
            String key=zoom+"/"+Math.floorMod(tx,world)+"/"+ty;
            BufferedImage tile=tiles.get(key);
            if(tile!=null)a.drawImage(tile,x,y,null);else loadTile(key);
        }
        a.setStroke(new BasicStroke(2));
        for(POIDTO p:pois){Point2D pos=screen(p);int x=(int)pos.getX(),y=(int)pos.getY();if(x < -130||x>w+130||y < -20||y>h+40)continue;
            boolean sel=selected!=null&&selected.id()==p.id();
            a.setColor(sel?new Color(204,104,40):new Color(22,111,75));a.fillOval(x-10,y-10,20,20);
            a.setColor(Color.WHITE);a.setStroke(new BasicStroke(2));a.drawOval(x-10,y-10,20,20);
            FontMetrics fm=a.getFontMetrics();int tw=fm.stringWidth(p.name());
            a.setColor(new Color(255,255,255,238));a.fillRoundRect(x-tw/2-7,y+12,tw+14,22,9,9);
            a.setColor(new Color(25,55,45));a.drawString(p.name(),x-tw/2,y+28);
        }
        a.setColor(new Color(255,255,255,225));a.fillRoundRect(8,8,190,49,10,10);
        a.setColor(new Color(38,64,54));a.drawString("OpenStreetMap · Z"+zoom,18,26);
        a.drawString(String.format(Locale.US,"%.5f, %.5f",lat,lon),18,44);
        a.setColor(new Color(255,255,255,235));a.fillRoundRect(8,h-36,330,28,8,8);
        a.setColor(new Color(54,68,59));a.drawString("© OpenStreetMap contributors · Kéo để di chuyển",15,h-17);
        a.dispose();}
    private void loadTile(String key){if(tiles.size()>1300 || !loading.add(key))return;
        pool.execute(()->{try{
            HttpRequest req=HttpRequest.newBuilder(URI.create("https://tile.openstreetmap.org/"+key+".png"))
                    .header("User-Agent","SGU-Client/1.0 (university demo; educational project)")
                    .timeout(Duration.ofSeconds(5)).build();
            HttpResponse<byte[]> res=http.send(req,HttpResponse.BodyHandlers.ofByteArray());
            if(res.statusCode()==200&&res.body().length>0){BufferedImage b=ImageIO.read(new java.io.ByteArrayInputStream(res.body()));if(b!=null){tiles.put(key,b);SwingUtilities.invokeLater(this::repaint);}}
        }catch(Exception ignored){/* offline grid fallback */}finally{loading.remove(key);}});
    }
    public void shutdown(){pool.shutdownNow();}
}
