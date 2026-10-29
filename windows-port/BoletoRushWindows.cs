using System;
using System.IO;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Reflection;
using System.Windows.Forms;
using System.Collections.Generic;
using System.Diagnostics;

namespace BoletoRushWindows {
    enum Screen { Menu, Tutorial, Playing, Paused, Revive, End }
    enum Kind { Coin, Bill, Shield, Heart }
    class Drop { public float X, Y, Speed; public Kind Kind; public Drop(float x, float y, float speed, Kind kind) { X=x;Y=y;Speed=speed;Kind=kind; } }
    class Zone { public RectangleF Rect; public string Id; public Zone(RectangleF r,string id) { Rect=r;Id=id; } }

    public class RushGame : Form {
        const float W=480f,H=820f;
        readonly Color bg=Color.FromArgb(7,19,38), teal=Color.FromArgb(37,226,209),
            gold=Color.FromArgb(255,209,102), coral=Color.FromArgb(255,101,124),
            muted=Color.FromArgb(167,187,207);
        readonly Dictionary<string,Bitmap> art=new Dictionary<string,Bitmap>();
        readonly List<Drop> drops=new List<Drop>();
        readonly List<Zone> zones=new List<Zone>();
        readonly Random rng=new Random();
        readonly Timer timer=new Timer();
        readonly Stopwatch clock=new Stopwatch();
        Screen screen=Screen.Menu;
        bool marathon=false, sound=true, won=false, revived=false, testMode=false;
        bool leftHeld=false, rightHeld=false;
        int score=0, bestClassic=0, bestMarathon=0, lives=3, combo=0, maxCombo=0, coins=0, bills=0, games=0;
        float elapsed=0, spawn=0, playerX=.5f, targetX=.5f, shield=0, invuln=0, feedbackTime=0;
        string feedback="";
        long lastTicks;
        float scale=1, left=0, top=0;
        readonly string savePath=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"ProximaEra","BoletoRushWindows","scores.txt");

        public RushGame() {
            Text="Boleto Rush! - Windows v1.4 (teste)";
            ClientSize=new Size(510,880);
            MinimumSize=new Size(410,700);
            StartPosition=FormStartPosition.CenterScreen;
            BackColor=bg;
            DoubleBuffered=true;
            KeyPreview=true;
            try { Icon=Icon.ExtractAssociatedIcon(Application.ExecutablePath); } catch {}
            LoadArt();
            LoadScores();
            timer.Interval=16;
            timer.Tick+=(s,e)=>TickGame();
            clock.Start();lastTicks=clock.ElapsedTicks;timer.Start();
            MouseDown+=(s,e)=>HandleMouse(e.X,e.Y,true);
            MouseMove+=(s,e)=>{ if (e.Button==MouseButtons.Left) HandleMouse(e.X,e.Y,false); };
            KeyDown+=HandleKey;
            Deactivate+=(s,e)=>{ leftHeld=false;rightHeld=false;if(screen==Screen.Playing) screen=Screen.Paused; Invalidate(); };
        }
        void LoadArt() {
            string[] names={
                "premium_promo.jpg","premium_running.jpg",
                "spr_hero_idle.png","spr_hero_run.png","spr_hero_jump.png","spr_hero_hurt.png",
                "spr_bill_idle.png","spr_bill_attack.png","spr_bill_damaged.png","spr_bill_defeated.png",
                "spr_coin_front.png","spr_coin_angle.png","spr_coin_side.png","spr_coin_collect.png",
                "spr_shield_idle.png","spr_shield_active.png","spr_shield_collect.png","spr_shield_impact.png"
            };
            foreach(string name in names) {
                try {
                    using(Stream stream=Assembly.GetExecutingAssembly().GetManifestResourceStream("rush."+name)) {
                        if(stream==null) continue;
                        using(Image img=Image.FromStream(stream)) {
                            int tw=img.Width,th=img.Height;
                            double factor=Math.Min(1.0,1400.0/Math.Max(tw,th));
                            Bitmap resized=new Bitmap(Math.Max(1,(int)(tw*factor)),Math.Max(1,(int)(th*factor)));
                            using(Graphics g=Graphics.FromImage(resized)) {
                                g.InterpolationMode=InterpolationMode.HighQualityBicubic;
                                g.DrawImage(img,0,0,resized.Width,resized.Height);
                            }
                            art[name]=resized;
                        }
                    }
                } catch(Exception ex) { Debug.WriteLine("Asset "+name+" "+ex.Message); }
            }
        }
        void LoadScores() {
            try { string[] lines=File.ReadAllLines(savePath);
                if(lines.Length>=3) {int.TryParse(lines[0],out bestClassic);int.TryParse(lines[1],out bestMarathon);int.TryParse(lines[2],out games);}
            }catch{}
        }
        void SaveScores() {
            try { Directory.CreateDirectory(Path.GetDirectoryName(savePath));
                File.WriteAllLines(savePath,new string[]{bestClassic.ToString(),bestMarathon.ToString(),games.ToString()});
            }catch{}
        }
        void StartGame() {
            score=0;lives=3;combo=0;maxCombo=0;coins=0;bills=0;
            leftHeld=false;rightHeld=false;
            elapsed=0;spawn=0;playerX=.5f;targetX=.5f;shield=0;invuln=0;
            feedback="";feedbackTime=0;won=false;revived=false;drops.Clear();
            screen=Screen.Playing;Invalidate();
        }
        void EndGame(bool victory) {
            won=victory;screen=Screen.End;games++;
            if(marathon) bestMarathon=Math.Max(bestMarathon,score);
            else bestClassic=Math.Max(bestClassic,score);
            if(!testMode) SaveScores();
        }
        void Revive() { revived=true;lives=1;invuln=2.5f;drops.Clear();feedback="SEGUNDA CHANCE!";feedbackTime=1.2f;screen=Screen.Playing; }
        void TickGame() {
            long now=clock.ElapsedTicks;
            float dt=Math.Max(0,Math.Min(.05f,(now-lastTicks)/(float)Stopwatch.Frequency));
            lastTicks=now;
            if(screen==Screen.Playing) UpdateGame(dt);
            Invalidate();
        }
        void PlayEffect(Kind kind) {
            if(!sound) return;
            try {
                if(kind==Kind.Bill) System.Media.SystemSounds.Hand.Play();
                else if(kind==Kind.Coin) System.Media.SystemSounds.Asterisk.Play();
                else System.Media.SystemSounds.Exclamation.Play();
            } catch {}
        }
        void UpdateGame(float dt) {
            float duration=marathon?90f:45f;
            elapsed+=dt;
            if(elapsed>=duration) { elapsed=duration;EndGame(true);return; }
            // Continuous keyboard movement is frame-rate independent.
            if(leftHeld && !rightHeld) targetX-=dt*1.5f;
            if(rightHeld && !leftHeld) targetX+=dt*1.5f;
            targetX=Math.Max(.09f,Math.Min(.91f,targetX));
            playerX+=(targetX-playerX)*Math.Min(1f,dt*17f);
            playerX=Math.Max(.09f,Math.Min(.91f,playerX));
            shield=Math.Max(0f,shield-dt);invuln=Math.Max(0f,invuln-dt);feedbackTime=Math.Max(0f,feedbackTime-dt);
            spawn+=dt;
            float interval=Math.Max(marathon?.32f:.39f,.90f-elapsed*(marathon?.007f:.009f));
            if(spawn>=interval) {
                spawn-=interval;double roll=rng.NextDouble();
                Kind k=roll<.045?Kind.Shield:roll<.075?Kind.Heart:
                    (elapsed<5 && roll>=.40)?Kind.Coin:roll<.56?Kind.Coin:Kind.Bill;
                drops.Add(new Drop(.09f+(float)rng.NextDouble()*.82f,-.065f,.27f+(float)rng.NextDouble()*.08f+elapsed*.0028f,k));
            }
            for(int i=drops.Count-1;i>=0;i--) {
                Drop item=drops[i];float old=item.Y;
                item.Y+=item.Speed*dt;
                if(old<.79f && item.Y>=.79f && Math.Abs(item.X-playerX)<.13f) {
                    switch(item.Kind) {
                        case Kind.Coin:
                            combo++;int gained=10+(Math.Min(combo,10)-1)*2;
                            score+=gained;coins++;maxCombo=Math.Max(maxCombo,combo);
                            feedback="+"+gained+"  MOEDA!";feedbackTime=.68f;break;
                        case Kind.Bill:
                            if(shield>0) {shield=0;feedback="ESCUDO SALVOU!";feedbackTime=.85f;}
                            else if(invuln<=0) {lives--;bills++;invuln=1.1f;combo=0;feedback="AI, MEU BOLSO!";feedbackTime=.85f;}
                            break;
                        case Kind.Shield:shield=7f;score+=5;feedback="ESCUDO ATIVADO!";feedbackTime=.85f;break;
                        case Kind.Heart:lives=testMode?lives:Math.Min(3,lives+1);score+=5;feedback="+1 VIDA!";feedbackTime=.85f;break;
                    }
                    PlayEffect(item.Kind);
                    drops.RemoveAt(i);
                } else if(item.Y>1.05f) drops.RemoveAt(i);
            }
            if(lives<=0) { if(!revived) screen=Screen.Revive;else EndGame(false); }
        }
        void HandleKey(object sender,KeyEventArgs e) {
            if(e.KeyCode==Keys.Escape || e.KeyCode==Keys.P) {
                if(screen==Screen.Playing) screen=Screen.Paused;
                else if(screen==Screen.Paused) screen=Screen.Playing;
                else if(screen==Screen.Tutorial) screen=Screen.Menu;
                else if(screen==Screen.End || screen==Screen.Revive) screen=Screen.Menu;
                e.Handled=true;
            }
            if(e.KeyCode==Keys.Left || e.KeyCode==Keys.A) leftHeld=true;
            if(e.KeyCode==Keys.Right || e.KeyCode==Keys.D) rightHeld=true;
            if(e.KeyCode==Keys.Space || e.KeyCode==Keys.Enter) {
                if(screen==Screen.Menu || screen==Screen.End) StartGame();
                else if(screen==Screen.Paused) screen=Screen.Playing;
                else if(screen==Screen.Tutorial) screen=Screen.Menu;
            }
            if(e.KeyCode==Keys.M) sound=!sound;
            Invalidate();
        }
        protected override void OnKeyUp(KeyEventArgs e) {
            base.OnKeyUp(e);
            if(e.KeyCode==Keys.Left || e.KeyCode==Keys.A) leftHeld=false;
            if(e.KeyCode==Keys.Right || e.KeyCode==Keys.D) rightHeld=false;
        }
        void HandleMouse(int px,int py,bool click) {
            float x=(px-left)/scale,y=(py-top)/scale;
            if(x<0 || x>W || y<0 || y>H) return;
            if(screen==Screen.Playing && y>120 && (!click || zones.Find(z=>z.Rect.Contains(x,y))==null)) {
                targetX=Math.Max(.09f,Math.Min(.91f,x/W));
                return;
            }
            if(!click) return;
            string key=null;
            for(int i=zones.Count-1;i>=0;i--) if(zones[i].Rect.Contains(x,y)) {key=zones[i].Id;break;}
            if(key==null) return;
            switch(key) {
                case "start":StartGame();break;
                case "mode":marathon=!marathon;break;
                case "sound":sound=!sound;break;
                case "how":screen=Screen.Tutorial;break;
                case "back":screen=Screen.Menu;break;
                case "pause":screen=Screen.Paused;break;
                case "resume":screen=Screen.Playing;break;
                case "restart":StartGame();break;
                case "revive":Revive();break;
                case "finish":EndGame(false);break;
            }
            Invalidate();
        }
        protected override void OnPaint(PaintEventArgs e) {
            base.OnPaint(e);
            Graphics g=e.Graphics;
            g.SmoothingMode=SmoothingMode.AntiAlias;
            g.InterpolationMode=InterpolationMode.HighQualityBicubic;
            g.TextRenderingHint=TextRenderingHint.AntiAliasGridFit;
            g.Clear(Color.FromArgb(3,10,18));
            scale=Math.Min(ClientSize.Width/W,ClientSize.Height/H);
            left=(ClientSize.Width-W*scale)/2f;
            top=(ClientSize.Height-H*scale)/2f;
            GraphicsState state=g.Save();
            g.TranslateTransform(left,top);
            g.ScaleTransform(scale,scale);
            zones.Clear();
            RenderBackground(g);
            if(screen==Screen.Menu) DrawMenu(g);
            else if(screen==Screen.Tutorial) DrawTutorial(g);
            else if(screen==Screen.Playing || screen==Screen.Paused) {
                DrawPlay(g);if(screen==Screen.Paused) DrawPause(g);
            } else if(screen==Screen.Revive) DrawRevive(g);
            else if(screen==Screen.End) DrawEnd(g);
            g.Restore(state);
        }
        void RenderBackground(Graphics g) {
            using(LinearGradientBrush brush=new LinearGradientBrush(new RectangleF(0,0,W,H),
                Color.FromArgb(18,48,77),bg,90f)) g.FillRectangle(brush,0,0,W,H);
            for(int i=0;i<19;i++) {
                float x=i*W/18f, bh=(i*29%9)*14+140;
                using(Brush b=new SolidBrush(Color.FromArgb(18,41,62)))g.FillRectangle(b,x,H-bh,Math.Max(7,W/23),bh);
                using(Brush b=new SolidBrush(Color.FromArgb(65,46,86,104)))
                    for(int j=0;j<5;j++)g.FillRectangle(b,x+3,H-bh+20+j*25,7,4);
            }
            using(Pen p=new Pen(Color.FromArgb(20,teal),1))
                for(int i=0;i<=16;i++)g.DrawLine(p,0,i*H/16,W,i*H/16);
            using(Pen p=new Pen(Color.FromArgb(80,teal),2))g.DrawLine(p,0,H*.90f,W,H*.90f);
        }
        void Txt(Graphics g,string text,float x,float y,float width,float height,float size,Color color,bool bold=false,StringAlignment align=StringAlignment.Center) {
            using(Font f=new Font("Segoe UI",size,bold?FontStyle.Bold:FontStyle.Regular,GraphicsUnit.Pixel))
            using(Brush b=new SolidBrush(color))
            using(StringFormat sf=new StringFormat()) {
                sf.Alignment=align;sf.LineAlignment=StringAlignment.Center;sf.Trimming=StringTrimming.EllipsisCharacter;
                g.DrawString(text,f,b,new RectangleF(x,y,width,height),sf);
            }
        }
        void Rect(Graphics g,RectangleF rect,Color fill,float radius=12) {
            using(GraphicsPath path=new GraphicsPath()) {
                float d=radius*2;
                path.AddArc(rect.X,rect.Y,d,d,180,90);
                path.AddArc(rect.Right-d,rect.Y,d,d,270,90);
                path.AddArc(rect.Right-d,rect.Bottom-d,d,d,0,90);
                path.AddArc(rect.X,rect.Bottom-d,d,d,90,90);
                path.CloseFigure();using(Brush b=new SolidBrush(fill))g.FillPath(b,path);
            }
        }
        void Button(Graphics g,string id,string text,float x,float y,float w,float h,bool secondary=false) {
            RectangleF r=new RectangleF(x,y,w,h);
            Rect(g,r,secondary?Color.FromArgb(30,60,80):Color.FromArgb(25,167,158),12);
            using(Pen p=new Pen(secondary?Color.FromArgb(95,teal):Color.FromArgb(180,teal),1.2f))
                g.DrawRectangle(p,r.X+1,r.Y+1,r.Width-2,r.Height-2);
            Txt(g,text,x+7,y,w-14,h,16,Color.White,true);
            zones.Add(new Zone(r,id));
        }
        void Sprite(Graphics g,string name,float x,float y,float width,float height) {
            Bitmap img;
            if(!art.TryGetValue(name,out img)) {using(Brush b=new SolidBrush(gold))g.FillEllipse(b,x-width/2,y-height/2,width,height);return;}
            g.DrawImage(img,new RectangleF(x-width/2,y-height/2,width,height),new RectangleF(0,0,img.Width,img.Height),GraphicsUnit.Pixel);
        }
        void DrawMenu(Graphics g) {
            Bitmap a;
            if(art.TryGetValue("premium_promo.jpg",out a))g.DrawImage(a,new RectangleF(0,0,W,275));
            using(LinearGradientBrush fade=new LinearGradientBrush(new Rectangle(0,220,480,125),Color.FromArgb(0,bg),bg,90f))
                g.FillRectangle(fade,0,220,480,125);
            Txt(g,"BOLETO RUSH!",20,270,440,55,40,Color.White,true);
            Txt(g,"O DESAFIO DA SUA CARTEIRA",15,320,450,28,16,gold,true);
            Txt(g,"Pegue moedas. Desvie dos boletos.",20,372,440,32,17,Color.White);
            Txt(g,"ESCUDO TEMPORARIO  •  VIDA EXTRA",20,412,440,27,13,muted,true);
            Txt(g,(marathon?"90":"45")+" SEGUNDOS  |  3 VIDAS  |  100% JOGAVEL NO PC",20,451,440,26,12,muted);
            Button(g,"mode",marathon?"MODO: MARATONA (90s)  ↻":"MODO: CLASSICO (45s)  ↻",70,493,340,49,true);
            Button(g,"start","JOGAR AGORA",70,555,340,61);
            Button(g,"how","COMO JOGAR",70,631,340,49,true);
            Button(g,"sound",sound?"SOM: LIGADO":"SOM: DESLIGADO",70,689,340,45,true);
            Txt(g,"RECORDE "+(marathon?bestMarathon:bestClassic)+"    •    PARTIDAS "+games,20,746,440,36,16,gold,true);
            Txt(g,"Windows v1.4 • Sem anuncios • ESC/P para pausar",15,790,450,23,11,muted);
        }
        void DrawPlay(Graphics g) {
            for(int i=0;i<drops.Count;i++) {
                Drop d=drops[i];float x=d.X*W,y=d.Y*H;
                if(d.Kind==Kind.Coin) Sprite(g,"spr_coin_front.png",x,y,54,54);
                if(d.Kind==Kind.Bill) Sprite(g,d.Y>.52?"spr_bill_attack.png":"spr_bill_idle.png",x,y,68,66);
                if(d.Kind==Kind.Shield) Sprite(g,"spr_shield_collect.png",x,y,65,65);
                if(d.Kind==Kind.Heart) {using(Brush b=new SolidBrush(coral))g.FillEllipse(b,x-22,y-22,44,44);Txt(g,"+",x-21,y-22,42,44,32,Color.White,true);}
            }
            float avatarX=playerX*W,avatarY=H*.79f;
            if(invuln<=0 || ((int)(invuln*11)%2==0)) {
                using(Brush b=new SolidBrush(Color.FromArgb(50,teal)))g.FillEllipse(b,avatarX-55,avatarY+58,110,16);
                if(shield>0) {
                    Sprite(g,"spr_shield_idle.png",avatarX,avatarY,128,130);
                    using(Pen p=new Pen(Color.FromArgb(160,teal),3))g.DrawEllipse(p,avatarX-62,avatarY-65,124,130);
                }
                string hero=invuln>0?"spr_hero_hurt.png":
                    Math.Abs(playerX-targetX)>.025?"spr_hero_run.png":"spr_hero_idle.png";
                Sprite(g,hero,avatarX,avatarY,112,116);
            }
            using(Brush b=new SolidBrush(Color.FromArgb(160,9,26,44)))g.FillRectangle(b,0,0,W,142);
            Txt(g,"PONTOS",22,13,130,28,13,teal,true,StringAlignment.Near);
            Txt(g,score.ToString(),22,43,145,42,32,Color.White,true,StringAlignment.Near);
            Txt(g,"TEMPO",175,13,130,28,13,muted,true);
            int remaining=Math.Max(0,(int)((marathon?90f:45f)-elapsed+.99f));
            Txt(g,remaining+"s",175,43,130,42,29,Color.White,true);
            Txt(g,"VIDAS",335,13,122,28,13,coral,true,StringAlignment.Far);
            Txt(g,new string('♥',Math.Max(0,lives)),320,43,135,42,27,coral,true,StringAlignment.Far);
            Rect(g,new RectangleF(22,99,436,7),Color.FromArgb(45,75,93),3);
            Rect(g,new RectangleF(22,99,Math.Max(1,436*elapsed/(marathon?90f:45f)),7),teal,3);
            Txt(g,shield>0?"ESCUDO ATIVO "+(int)Math.Ceiling(shield)+"s":combo>=3?"COMBO x"+combo:"TOQUE OU ARRASTE PARA MOVER",20,113,325,34,13,shield>0||combo>=3?gold:muted,true,StringAlignment.Near);
            Button(g,"pause","PAUSAR",358,117,98,30,true);
            if(feedbackTime>0) Txt(g,feedback,15,273,450,68,23,feedback.Contains("BOLSO")?coral:gold,true);
            Txt(g,"MOUSE / TOQUE / SETAS / A-D PARA MOVER",15,775,450,29,13,Color.White);
        }
        void Overlay(Graphics g) {using(Brush b=new SolidBrush(Color.FromArgb(224,bg)))g.FillRectangle(b,0,0,W,H);}
        void DrawTutorial(Graphics g) {
            Overlay(g);Txt(g,"COMO JOGAR",20,96,440,65,37,teal,true);
            Txt(g,"MOVA A CARTEIRA",20,192,440,38,22,gold,true);
            Txt(g,"Arraste o mouse, toque na tela ou use A / D e setas.",35,231,410,57,17,Color.White);
            Txt(g,"MOEDAS = PONTOS",20,305,440,39,22,gold,true);
            Txt(g,"Colete moedas em sequencia para multiplicar a pontuacao.",35,349,410,58,17,Color.White);
            Txt(g,"BOLETOS = PERIGO",20,431,440,39,22,coral,true);
            Txt(g,"Cada boleto tira uma vida. O escudo protege uma vez.",35,479,410,57,17,Color.White);
            Txt(g,"CLASSICO 45s  |  MARATONA 90s",20,552,440,45,18,teal,true);
            Txt(g,"Sobreviva ate o tempo acabar. Pause com P ou ESC.",35,606,410,50,17,Color.White);
            Button(g,"back","ENTENDI!",75,697,330,65);
        }
        void DrawPause(Graphics g) {
            Overlay(g);Txt(g,"PAUSA",30,191,420,89,56,Color.White,true);
            Txt(g,"Sua carteira merece um descanso.",30,285,420,39,18,muted);
            Button(g,"resume","CONTINUAR",75,378,330,59);
            Button(g,"restart","RECOMECAR",75,452,330,55,true);
            Button(g,"sound",sound?"SOM: LIGADO":"SOM: DESLIGADO",75,523,330,55,true);
            Button(g,"back","MENU PRINCIPAL",75,595,330,55,true);
        }
        void DrawRevive(Graphics g) {
            Overlay(g);Sprite(g,"premium_running.jpg",W/2,177,370,207);
            Txt(g,"A FATURA CHEGOU!",15,313,450,66,32,coral,true);
            Txt(g,"Mas ainda da para escapar!",20,383,440,38,20,gold,true);
            Txt(g,score+" PONTOS  •  "+(marathon?"MARATONA":"CLASSICO"),20,436,440,43,22,Color.White,true);
            Txt(g,"TESTE NO WINDOWS: uma segunda chance gratuita.",40,510,400,54,18,Color.White);
            Txt(g,"No aplicativo Android, a recuperacao usa anuncio opcional.",35,563,410,52,15,muted);
            Button(g,"revive","REVIVER COM 1 VIDA",70,643,340,64);
            Button(g,"finish","ENCERRAR PARTIDA",70,724,340,55,true);
        }
        void DrawEnd(Graphics g) {
            Overlay(g);Sprite(g,"premium_running.jpg",W/2,170,350,195);
            Txt(g,won?"SOBREVIVEU!":"A FATURA CHEGOU!",20,299,440,65,35,won?teal:coral,true);
            Txt(g,score.ToString(),20,366,440,105,86,Color.White,true);
            Txt(g,"PONTOS  •  "+(marathon?"MARATONA":"CLASSICO"),20,470,440,33,19,gold,true);
            Txt(g,"RECORDE: "+(marathon?bestMarathon:bestClassic),30,526,420,47,22,teal,true);
            Txt(g,"MOEDAS "+coins+"  •  BOLETOS "+bills+"  •  COMBO MAX. "+maxCombo,20,585,440,42,15,muted);
            Button(g,"restart","JOGAR DE NOVO",75,664,330,63);
            Button(g,"back","MENU PRINCIPAL",75,748,330,53,true);
        }
        public bool SelfTest() {
            testMode=true;
            if(!art.ContainsKey("spr_hero_idle.png")||!art.ContainsKey("spr_coin_front.png")||!art.ContainsKey("premium_promo.jpg")) throw new Exception("Artes essenciais ausentes.");
            StartGame();lives=100;
            for(int i=0;i<400;i++)UpdateGame(.016f);
            if(screen!=Screen.Playing || elapsed<=0 || lives<1) throw new Exception("Falha na simulacao de partida.");
            screen=Screen.Revive;Revive();
            if(screen!=Screen.Playing || lives!=1 || !revived) throw new Exception("Falha na retomada.");
            EndGame(true);
            if(screen!=Screen.End)throw new Exception("Falha no fim de partida.");
            sound=false;
            marathon=false;
            StartGame();lives=1000;
            for(int i=0;i<1000 && screen==Screen.Playing;i++)UpdateGame(.05f);
            if(screen!=Screen.End || !won || elapsed<45f) throw new Exception("Falha no modo classico completo.");
            marathon=true;
            StartGame();lives=1000;
            for(int i=0;i<2000 && screen==Screen.Playing;i++)UpdateGame(.05f);
            if(screen!=Screen.End || !won || elapsed<90f) throw new Exception("Falha no modo maratona completo.");
            return true;
        }
        protected override void Dispose(bool disposing) {
            if(disposing) {timer.Stop();timer.Dispose();foreach(var b in art.Values)b.Dispose();}
            base.Dispose(disposing);
        }
    }
    static class Program {
        [STAThread] static int Main(string[] args) {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            if(args.Length>0 && args[0]=="--self-test") {
                try {using(RushGame game=new RushGame())game.SelfTest();Console.WriteLine("SMOKE PASS");return 0;}
                catch(Exception ex){try{File.WriteAllText(Path.Combine(Path.GetTempPath(),"BoletoRush-smoke-error.txt"),ex.ToString());}catch{} Console.Error.WriteLine(ex.ToString());return 2;}
            }
            Application.Run(new RushGame());return 0;
        }
    }
}
