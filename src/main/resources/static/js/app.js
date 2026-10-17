// ---------- Language (saved in the browser; for logged-in users also saved to their account)
const LANGS={en:"English",mr:"मराठी",hi:"हिन्दी"};
let me=null;                                   // the logged-in user, set by initPage()
const $=id=>document.getElementById(id);
function lang(){return localStorage.getItem("lang")||"en";}
function t(k){return (T[lang()]&&T[lang()][k])||T.en[k]||k;}
function setLang(l){localStorage.setItem("lang",l);applyLang();if(me)api("/api/profile/language","PUT",{language:l.toUpperCase()});}
function applyLang(){
  document.documentElement.lang=lang();
  document.querySelectorAll("[data-i18n]").forEach(e=>e.textContent=t(e.dataset.i18n));
  document.querySelectorAll("[data-i18n-ph]").forEach(e=>e.placeholder=t(e.dataset.i18nPh));
  document.querySelectorAll(".lang-select").forEach(s=>s.value=lang());
  if(window.onLang)window.onLang();
}
// ---------- API helper: always returns {ok,status,data}
async function api(path,method="GET",body){
  const o={method,headers:{},credentials:"same-origin"};
  if(body!==undefined){o.headers["Content-Type"]="application/json";o.body=JSON.stringify(body);}
  try{const r=await fetch(path,o);let d=null;try{d=await r.json();}catch(e){}return{ok:r.ok,status:r.status,data:d};}
  catch(e){return{ok:false,status:0,data:null};}
}
const ERR={EMAIL_EXISTS:"emailExists",BAD_CREDENTIALS:"loginFailed",ACCOUNT_DISABLED:"accountDisabled",VALIDATION:"required",CATEGORY_EXISTS:"categoryExists",INVALID_IMAGE:"invalidImage",TOO_MANY_IMAGES:"tooManyImages",LISTING_IN_USE:"listingInUse",FORBIDDEN:"forbidden",NOT_FOUND:"notFound",INVALID_PRICE_RANGE:"priceRange",APPOINTMENT_OPEN:"appointmentOpen",PAST_DATE:"pastDate",INVALID_TRANSITION:"invalidTransition",TOO_EARLY:"tooEarly",MEETING_REQUIRED:"meetingRequired",SUPPLY_EXCEEDS:"supplyExceeds",PAYMENT_EXCEEDS:"paymentExceeds",NO_SUPPLY:"noSupply",REVIEW_EXISTS:"reviewExists",FUTURE_DATE:"futureDate",ALREADY_REPORTED:"alreadyReported",OWN_REQUEST:"ownRequest",COMPLETION_PENDING:"completionPending",REQUEST_NOT_PENDING:"requestNotPending",ADMIN_PROTECTED:"adminProtected",COMPLAINT_CLOSED:"complaintClosed"};
const errMsg=r=>t((r.data&&ERR[r.data.code])||"serverError");
const homeFor=role=>({FARMER:"farmer.html",MERCHANT:"merchant.html",ADMIN:"admin.html"})[role];
// Call at the top of every protected page. Returns true if the right user is logged in.
async function initPage(role){
  const r=await api("/api/auth/me");
  if(!r.ok){location.href="login.html";return false;}
  me=r.data;
  const allowed=Array.isArray(role)?role.includes(me.role):me.role===role;
  if(!allowed){location.href=homeFor(me.role);return false;}
  const h=$("hdr");                                   // shared pages: build the menu once we know the role
  if(h&&h.dataset.role=="auto"){h.dataset.role=me.role.toLowerCase();buildHeader();}
  localStorage.setItem("lang",me.preferredLanguage.toLowerCase());
  applyLang();return true;
}
async function logout(){await api("/api/auth/logout","POST");location.href="login.html";}
// ---------- Header
function buildHeader(){
  const h=$("hdr");if(!h)return;
  const role=h.dataset.role||"";
  const nav={farmer:[["farmer.html#find","findMerchants"],["farmer.html#myfruits","myFruits"],["deals.html","dealsPay"],["meetings.html","meetings"],["myprofile.html","profile"]],
    merchant:[["merchant.html#myreqs","myReqs"],["merchant.html#find","findFarmers"],["deals.html","dealsPay"],["meetings.html","meetings"],["myprofile.html","profile"]],
    admin:[["admin.html","adminTitle"]]}[role]||[];
  h.className="top";
  h.innerHTML='<span class="brand">🌿 AgroConnect</span>'+
    (nav.length?'<nav>'+nav.map((n,i)=>`<a href="${n[0]}" data-i18n="${n[1]}" class="${i==0?"on":""}"></a>`).join("")+
    '<a href="#" onclick="logout();return false" data-i18n="logout"></a></nav>':"")+
    '<select class="lang-select" aria-label="Language" onchange="setLang(this.value)">'+
    Object.keys(LANGS).map(k=>`<option value="${k}">${LANGS[k]}</option>`).join("")+"</select>";
}
// ---------- Helpers
const esc=s=>String(s==null?"":s).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"})[c]);
const fmtKg=n=>n.toLocaleString("en-IN")+" "+t("kg")+(n>=1000?" ("+(n/1000)+" "+t("tons")+")":"");
const stars=r=>r==null?t("noRatings"):'<span class="stars">★ '+r.toFixed(1)+"</span>";
const initials=s=>s.split(" ").slice(0,2).map(w=>w[0]).join("").toUpperCase();
const catName=c=>(lang()=="mr"&&c.nameMr)||(lang()=="hi"&&c.nameHi)||c.name;
const avatarFor=(id,url)=>url||"/images/avatars/avatar-"+((id%4)+1)+".svg";
const STATUS={ACTIVE:"active",PAUSED:"paused",SOLD_OUT:"soldOut",CLOSED:"closed",PENDING:"pending",CONFIRMED:"confirmed",
 RESCHEDULE_REQUESTED:"rescheduleReq",REJECTED:"rejected",CANCELLED:"cancelled",COMPLETED:"completed",
 PENDING_CONFIRMATION:"waitConfirm",RECORDED:"recorded",PENDING_VERIFICATION:"pendingVerif",VERIFIED:"verified"};
const deb=(fn,ms=300)=>{let h;return(...x)=>{clearTimeout(h);h=setTimeout(()=>fn(...x),ms);};};
const qtyToKg=(v,unit)=>unit=="tons"?Math.round(v*1000*100)/100:+v;
const price=(lo,hi)=>"₹"+(lo?lo+"–":"")+hi+"/"+t("kg");
const thumbs=imgs=>imgs&&imgs.length?'<div class="thumbs">'+imgs.map(i=>`<img src="${esc(i.url)}" alt="" loading="lazy">`).join("")+"</div>":"";
const rupee=n=>"₹"+Number(n).toLocaleString("en-IN",{maximumFractionDigits:2});
// Small "deals" box shown on both dashboards (element id="dls")
const dealsSummary={data:null,
 async load(){const r=await api("/api/deals");this.data=r.ok?r.data:[];this.draw();},
 draw(){
  const el=$("dls");if(!el)return;
  const d=(this.data||[]).filter(x=>x.status=="ACTIVE"||x.status=="PENDING_CONFIRMATION").slice(0,3);
  if(!d.length){el.innerHTML='<div class="empty">'+t("emptyDeals")+"</div>";return;}
  el.innerHTML=d.map(x=>`<div class="card" style="margin-bottom:.5rem"><strong>${esc(x.other.displayName)}</strong> · ${esc(catName(x.category))}
   <span class="badge">${t(STATUS[x.status])}</span><br>${t("remainingQty")}: ${fmtKg(x.summary.remainingKg)} · ${t("outstandingAmt")}: ${rupee(x.summary.outstanding)}</div>`).join("")+
   `<a class="btn blue" href="deals.html">${t("dealsPay")}</a>`;}};
const todayStr=()=>new Date(Date.now()-new Date().getTimezoneOffset()*60000).toISOString().slice(0,10);
// Small "meetings" box shown on both dashboards (element id="mtg")
const mtgSummary={data:null,
 async load(){const r=await api("/api/appointments");this.data=r.ok?r.data:[];this.draw();},
 draw(){
  const el=$("mtg");if(!el)return;const d=this.data||[];
  if(!d.some(x=>["PENDING","RESCHEDULE_REQUESTED","CONFIRMED"].includes(x.status))){el.innerHTML='<div class="empty">'+t("emptyAppts")+"</div>";return;}
  const need=d.filter(x=>x.canRespond).length,next=d.filter(x=>x.status=="CONFIRMED").slice(0,3);
  el.innerHTML='<div class="card">'+(need?`<p><strong>${need}</strong> ${t("needReply")}</p>`:"")+
   next.map(x=>`<p>📅 ${esc(x.date)} ${esc(x.time.slice(0,5))} · ${esc(x.other.displayName)} <span class="badge">${t(STATUS[x.status])}</span></p>`).join("")+
   `<a class="btn ghost" href="meetings.html">${t("viewAll")}</a></div>`;}};
buildHeader();applyLang();
