package com.sgu.audioguide.util;
/** MPEG Layer III frame scanner. Validates actual audio frames, skips ID3 tags and totals duration. */
public final class MP3 {
    private MP3() {
    }
    public record Info(double duration,int frames) {
    }
    public static Info inspect(byte[] a) {
        int pos=0;
        if(a.length>10&&a[0]=='I'&&a[1]=='D'&&a[2]=='3') {
            for(int k=6;k<10;k++)if((a[k]&128)!=0)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid ID3 tag");
            pos=10+((a[6]&127)<<21)+((a[7]&127)<<14)+((a[8]&127)<<7)+(a[9]&127);
            if((a[5]&16)!=0)pos+=10;
        } int frames=0,skipped=0;
        double seconds=0;
        while(pos+4<a.length) {
            int b0=a[pos]&255,b1=a[pos+1]&255,b2=a[pos+2]&255;
            int ver=(b1>>3)&3,layer=(b1>>1)&3,br=(b2>>4)&15,sr=(b2>>2)&3;
            if(b0!=255||(b1&224)!=224||ver==1||layer!=1||br==0||br==15||sr==3) {
                if(frames>0)break;
                pos++;
                if(++skipped>4096)break;
                continue;
            }
            int rate=new int[] {
                44100,48000,32000
            }
            [sr];
            if(ver==2)rate/=2;
            if(ver==0)rate/=4;
            int kbps=(ver==3?new int[] {
                0,32,40,48,56,64,80,96,112,128,160,192,224,256,320
            }
            :new int[] {
                0,8,16,24,32,40,48,56,64,80,96,112,128,144,160
            })[br];
            int frame=(ver==3?144:72)*kbps*1000/rate+((b2>>1)&1);
            if(pos+frame>a.length)break;
            frames++;
            seconds+=(ver==3?1152.0:576.0)/rate;
            pos+=frame;
        } if(frames<3||seconds<0.05)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"File không phải MP3 hợp lệ. Không chỉ đổi đuôi file thành .mp3.");
        return new Info(Math.round(seconds*100.0)/100.0,frames);
    }
}
