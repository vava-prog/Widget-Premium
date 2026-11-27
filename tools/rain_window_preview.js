// Bản vẽ thử bằng canvas 2D của SkyArt.rainWindow và WidgetBackground.glassRim (Kotlin): cùng hằng số, cùng thứ tự vẽ.
// Chỉ để xem trước bố cục trên máy (tools/preview_widget_mock.py); bộ vẽ thật là Canvas của Android.
// BlurMaskFilter bán kính r của Android tương đương độ lệch chuẩn CSS blur = 0.577 * r + 0.5.
function sigma(r){return r>0?0.577*r+0.5:0}
function rgba(hex,a){const n=parseInt(hex.slice(1),16);return `rgba(${n>>16&255},${n>>8&255},${n&255},${Math.max(0,Math.min(1,a))})`}
function mulberry(seed){let a=seed>>>0;return function(){a|=0;a=a+0x6D2B79F5|0;let t=Math.imul(a^a>>>15,1|a);t=t+Math.imul(t^t>>>7,61|t)^t;return((t^t>>>14)>>>0)/4294967296}}

function blobCircle(c,x,y,r,color,alpha,blur){c.save();c.filter=`blur(${sigma(blur)}px)`;c.fillStyle=rgba(color,alpha);c.beginPath();c.arc(x,y,r,0,7);c.fill();c.restore()}
function blobRect(c,x0,y0,x1,y1,color,alpha,blur){c.save();c.filter=`blur(${sigma(blur)}px)`;c.fillStyle=rgba(color,alpha);c.fillRect(x0,y0,x1-x0,y1-y0);c.restore()}
function glow(c,cx,cy,r,color,alpha){const g=c.createRadialGradient(cx,cy,0,cx,cy,r);g.addColorStop(0,rgba(color,alpha));g.addColorStop(.4,rgba(color,alpha*.35));g.addColorStop(1,rgba(color,0));c.fillStyle=g;c.beginPath();c.arc(cx,cy,r,0,7);c.fill()}

function rainWindow(c,w,h,unit,alpha,night,animated){
  const rnd=mulberry(41);
  // 1. nền trời: xanh xám nhạt ở trên, đậm dần xuống dưới
  let g=c.createLinearGradient(0,0,0,h);
  if(!night){g.addColorStop(0,rgba('#86A9D8',alpha));g.addColorStop(.55,rgba('#6F93C2',alpha));g.addColorStop(1,rgba('#4F7399',alpha))}
  else{g.addColorStop(0,rgba('#32486B',alpha));g.addColorStop(.55,rgba('#263A58',alpha));g.addColorStop(1,rgba('#18263C',alpha))}
  c.fillStyle=g;c.fillRect(0,0,w,h);
  // 2. toà nhà mờ phía sau
  const bt=night?'#27405F':'#3F6797', bl=night?'#3A5679':'#6F97C8';
  blobRect(c,w*.60,h*.20,w*.80,h*1.1,bt,alpha*.80,h*.05);
  blobRect(c,w*.80,h*.06,w*.97,h*1.1,bl,alpha*.70,h*.05);
  blobRect(c,w*.44,h*.46,w*.58,h*1.1,bt,alpha*.60,h*.05);
  blobRect(c,w*.30,h*.58,w*.42,h*1.1,bl,alpha*.45,h*.05);
  // ô cửa sáng nhỏ trên toà nhà
  for(let i=0;i<14;i++){const x=w*(.62+rnd()*.33),y=h*(.28+rnd()*.5),s=(2+rnd()*3)*unit;
    blobRect(c,x,y,x+s,y+s*1.3,'#DCEBFA',alpha*(night?.30:.22),2.2*unit)}
  // 3. cây xanh mờ: trái, trên-trái, phải-dưới
  const greens=night?['#1F4A33','#28603E','#1B3B2C']:['#2C6B3A','#4C9444','#7DB45A','#235A45'];
  const clusters=[[.03,.18,.20],[.14,.06,.16],[.07,.50,.19],[.20,.32,.13],[.02,.80,.16],[.92,.34,.10],[.97,.62,.12],[.84,.90,.20],[.96,.94,.20],[.30,.02,.11]];
  for(const [cx,cy,cr] of clusters){const n=5;for(let i=0;i<n;i++){
    const a=rnd()*6.283,d=rnd()*cr*h*.6;
    blobCircle(c,w*cx+Math.cos(a)*d,h*cy+Math.sin(a)*d,h*cr*(.45+rnd()*.5),greens[Math.floor(rnd()*greens.length)],alpha*.78,h*.07)}}
  // 4. quầng sáng ấm góc dưới trái (đèn xe/biển hiệu)
  glow(c,w*.05,h*.98,h*.62,'#FFB84D',alpha*(night?.6:.78));
  glow(c,w*.16,h*1.02,h*.36,'#FFE2A8',alpha*(night?.4:.5));
  for(let i=0;i<4;i++)blobCircle(c,w*(.06+i*.05+rnd()*.03),h*(.78+rnd()*.18),(5+rnd()*5)*unit,'#FFE9BB',alpha*.28,2.5*unit);
  // 5. phủ tối nhẹ để chữ trắng luôn đọc được
  g=c.createLinearGradient(0,0,0,h);
  g.addColorStop(0,rgba('#102238',alpha*(night?.30:.18)));g.addColorStop(1,rgba('#0B1828',alpha*(night?.38:.30)));
  c.fillStyle=g;c.fillRect(0,0,w,h);
  // 6. hạt nước li ti bám kính (giọt thấu kính)
  const n=animated?84:120;
  for(let i=0;i<n;i++){
    const x=rnd()*w,y=rnd()*h,r=(.8+rnd()*rnd()*2.2)*unit*(rnd()<.14?1.8:1);
    microDrop(c,x,y,r,alpha);
  }
  if(!animated){ // không có lớp chuyển động: vẽ sẵn vài vệt nước có giọt ở đầu dưới
    const xs=[.12,.31,.52,.70,.88];
    xs.forEach((fx,i)=>{const x=w*(fx+(rnd()-.5)*.06),y0=h*(.04+rnd()*.12),len=h*(.35+rnd()*.3);trail(c,x,y0,len,unit,alpha,rnd);
      bigDrop(c,x,y0+len,(2.6+rnd()*1.4)*unit,alpha)});
  }
}
function microDrop(c,x,y,r,alpha){
  c.save();
  c.fillStyle=rgba('#FFFFFF',alpha*.07);c.beginPath();c.arc(x,y,r,0,7);c.fill();
  c.lineWidth=Math.max(.5,r*.18);c.strokeStyle=rgba('#0B1828',alpha*.34);c.beginPath();c.arc(x,y,r,3.4,5.4);c.stroke();   // mép tối trên-trái
  c.strokeStyle=rgba('#FFFFFF',alpha*.62);c.beginPath();c.arc(x,y,r*.82,.35,2.2);c.stroke();                                  // mép sáng dưới-phải
  c.fillStyle=rgba('#FFFFFF',alpha*.9);c.beginPath();c.arc(x-r*.34,y-r*.38,Math.max(.4,r*.24),0,7);c.fill();                  // điểm sáng
  c.restore();
}
function bigDrop(c,x,y,r,alpha){
  c.save();
  const g=c.createLinearGradient(x,y-r,x,y+r*1.1);g.addColorStop(0,rgba('#0B1828',alpha*.30));g.addColorStop(.5,rgba('#FFFFFF',alpha*.05));g.addColorStop(1,rgba('#FFFFFF',alpha*.40));
  c.beginPath();c.moveTo(x,y-r*1.7);c.lineTo(x+r*.93,y-r*.36);c.arc(x,y,r,-.38,Math.PI+.38);c.closePath();   // thân tròn + đuôi nhọn lên trên
  c.fillStyle=g;c.fill();c.lineWidth=Math.max(.6,r*.12);c.strokeStyle=rgba('#FFFFFF',alpha*.45);c.stroke();
  c.fillStyle=rgba('#FFFFFF',alpha*.95);c.beginPath();c.ellipse(x-r*.36,y-r*.3,r*.26,r*.16,-.5,0,7);c.fill();
  c.restore();
}
function trail(c,x,y0,len,unit,alpha,rnd){
  c.save();c.lineCap='round';
  const ph=rnd()*6.28;
  c.beginPath();
  for(let i=0;i<=24;i++){const f=i/24,xx=x+Math.sin(f*6+ph)*unit*.5,yy=y0+len*f;i?c.lineTo(xx,yy):c.moveTo(xx,yy)}
  c.strokeStyle=rgba('#FFFFFF',alpha*.20);c.lineWidth=unit*2.2;c.stroke();
  c.strokeStyle=rgba('#FFFFFF',alpha*.38);c.lineWidth=unit*.8;c.stroke();
  c.restore();
}

// Mép kính của nền thời tiết (WidgetBackground.glassRim)
function glassRim(c,w,h,radius,unit,alpha){
  const white=a=>`rgba(255,255,255,${a*alpha})`;
  const rr=(x,y,ww,hh,r)=>{c.beginPath();c.roundRect(x,y,ww,hh,Math.max(0,r))};
  let g=c.createLinearGradient(0,0,0,h*.45);g.addColorStop(0,white(.16));g.addColorStop(1,white(0));
  c.fillStyle=g;rr(0,0,w,h,radius);c.fill();
  c.save();c.filter=`blur(${sigma(2.2*unit)}px)`;c.strokeStyle=white(.10);c.lineWidth=3*unit;rr(2*unit,2*unit,w-4*unit,h-4*unit,radius-2*unit);c.stroke();c.restore();
  g=c.createLinearGradient(0,0,w,h);g.addColorStop(0,white(.70));g.addColorStop(.35,white(.16));g.addColorStop(.65,white(.16));g.addColorStop(1,white(.42));
  c.strokeStyle=g;c.lineWidth=1.3*unit;const hf=.65*unit;rr(hf,hf,w-2*hf,h-2*hf,radius);c.stroke();
}
