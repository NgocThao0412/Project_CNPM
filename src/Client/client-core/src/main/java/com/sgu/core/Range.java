package com.sgu.core;
/** RFC byte ranges, including suffix and open-ended forms required by mobile media players. */
public record Range(long start,long end,boolean partial) {
    public long length() {
        return end-start+1;
    }
    public static Range parse(String header,long size) {
        if(size<=0)throw new Http.Error(416,"Empty media");
        if(header==null||header.isBlank())return new Range(0,size-1,false);
        if(!header.matches("bytes=[0-9]*-[0-9]*"))throw new Http.Error(416,"Single byte range required");
        try {
            String[] p=header.substring(6).split("-",-1);
            long a,b;
            if(p[0].isEmpty()) {
                long n=Long.parseLong(p[1]);
                if(n<=0)throw new NumberFormatException();
                a=Math.max(0,size-n);
                b=size-1;
            }
            else {
                a=Long.parseLong(p[0]);
                b=p[1].isEmpty()?size-1:Math.min(size-1,Long.parseLong(p[1]));
            }
            if(a<0||a>=size||b<a)throw new NumberFormatException();
            return new Range(a,b,true);
        }
        catch(NumberFormatException e) {
            throw new Http.Error(416,"Unsatisfiable range");
        }
    }
}
