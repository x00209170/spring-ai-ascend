package com.bank.financial.research.web;

import com.bank.financial.research.data.DataIngestionService;
import com.bank.financial.research.data.FreshnessPolicy;
import com.bank.financial.research.data.ResearchDataSource;
import com.bank.financial.research.data.tushare.TushareResearchDataSource;
import com.bank.financial.research.data.stub.StubResearchDataSource;
import com.bank.financial.research.engine.PipelineProgress;
import com.bank.financial.research.engine.ReportRequest;
import com.bank.financial.research.engine.ResearchReport;
import com.bank.financial.research.engine.ResearchReportEngine;
import com.bank.financial.research.model.ScriptedReportModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ascend.a2a.memory.obs.MemoryObserver;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A self-contained, dependency-light web playground for the multi-agent research
 * engine. It runs the JDK's built-in {@link HttpServer} (no Spring): a single
 * config-and-preview page at {@code /}, and a Server-Sent-Events stream at
 * {@code /api/run} that drives one report run and emits per-agent progress as the
 * pipeline advances, then the rendered report.
 *
 * <p>The engine is wired in its offline "scripted model" shape so the demo is
 * deterministic and needs no live model. The data source is chosen per request:
 * Tushare (if {@code TUSHARE_TOKEN} is set), or the offline stub as a transparent
 * fallback for the not-yet-wired providers.
 *
 * <pre>
 *   mvn -pl financial exec:java -Dexec.mainClass=com.bank.financial.research.web.ResearchWebServer
 * </pre>
 */
public final class ResearchWebServer {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ResearchWebServer() {
    }

    public static void main(String[] args) throws IOException {
        com.bank.financial.research.LogQuieter.quiet();
        int port = parsePort(System.getenv("RESEARCH_WEB_PORT"), 8088);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/", ResearchWebServer::handlePage);
        server.createContext("/api/run", ResearchWebServer::handleRun);
        server.start();
        System.out.println("research web on http://localhost:" + port);
    }

    private static int parsePort(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ── GET / ─────────────────────────────────────────────────────────────────
    private static void handlePage(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }
            byte[] body = PAGE.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html;charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        } catch (IOException e) {
            // client gone — nothing to do
        } finally {
            ex.close();
        }
    }

    // ── GET /api/run  → SSE ─────────────────────────────────────────────────────
    private static void handleRun(HttpExchange ex) {
        OutputStream os = null;
        try {
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            String type = q.getOrDefault("type", "equity");
            String source = q.getOrDefault("source", "tushare");
            String ticker = orDefault(q.get("ticker"), "DEMO");
            long pace = parseLong(q.get("pace"), 250L);

            ex.getResponseHeaders().set("Content-Type", "text/event-stream;charset=utf-8");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.getResponseHeaders().set("Connection", "keep-alive");
            ex.sendResponseHeaders(200, 0);
            os = ex.getResponseBody();
            final OutputStream out = os;

            long now = System.currentTimeMillis();

            // ── choose the data source (transparent fallback to the offline stub) ──
            ResearchDataSource src;
            String token = System.getenv("TUSHARE_TOKEN");
            switch (source) {
                case "stub" -> src = new StubResearchDataSource(now);
                case "wind", "choice" -> {
                    sendNote(out, "该源规划中(sidecar 网关),演示回退桩数据。");
                    src = new StubResearchDataSource(now);
                }
                default -> { // "tushare"
                    if (token != null && !token.isBlank()) {
                        src = new TushareResearchDataSource(token, now);
                    } else {
                        sendNote(out, "Tushare 未配置 token,演示回退桩数据。");
                        src = new StubResearchDataSource(now);
                    }
                }
            }

            ResearchReportEngine engine = new ResearchReportEngine(
                    new DataIngestionService(src, FreshnessPolicy.days(90)), src.name(),
                    new ScriptedReportModel(), null, MemoryObserver.NOOP, () -> now);

            // ── progress callback: one SSE line per agent transition ──────────────
            PipelineProgress progress = (role, state, index, total) -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("role", role);
                data.put("state", state);
                data.put("index", index);
                data.put("total", total);
                send(out, "agent", data);
                if ("running".equals(state) && pace > 0) {
                    try {
                        Thread.sleep(pace);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            };

            ResearchReport r = engine.generate(ReportRequest.equity(ticker, "web", now), progress);

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("html", MdHtml.render(r.toMarkdown()));
            report.put("rating", r.rating());
            report.put("priceTarget", r.priceTarget());
            report.put("currentPrice", r.currentPrice());
            report.put("upsidePct", r.upsidePct());
            report.put("modelCalls", r.metadata().modelCalls());
            report.put("criticRounds", r.metadata().criticRounds());
            report.put("degradations", r.metadata().degradations().size());
            send(out, "report", report);

            send(out, "done", Map.of());
        } catch (Exception e) {
            if (os != null) {
                try {
                    send(os, "error", Map.of("message", String.valueOf(e.getMessage())));
                } catch (RuntimeException ignored) {
                    // client gone
                }
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
            ex.close();
        }
    }

    private static void sendNote(OutputStream out, String message) {
        send(out, "note", Map.of("message", message));
    }

    /** Write one SSE frame; swallow IO (a disconnected client must not crash the run). */
    private static void send(OutputStream out, String event, Map<String, ?> data) {
        try {
            String frame = "event: " + event + "\n"
                    + "data: " + JSON.writeValueAsString(data) + "\n\n";
            out.write(frame.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            // client disconnected mid-stream — stop trying to write
        } catch (RuntimeException e) {
            // serialization issue — best effort, don't abort the run
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> q = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return q;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                q.put(decode(pair), "");
            } else {
                q.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return q;
    }

    private static String decode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String orDefault(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static long parseLong(String v, long fallback) {
        if (v == null || v.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(0, Long.parseLong(v.trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ── the single-page app (inline CSS + JS, no external resources) ─────────────
    private static final String PAGE = """
            <!doctype html>
            <html lang="zh-CN">
            <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1"/>
            <title>研报生成 · 多智能体引擎</title>
            <style>
              :root{
                --bg:#0e1117; --panel:#161b22; --panel2:#1c232c; --border:#2a313c;
                --txt:#e6edf3; --muted:#8b949e; --accent:#3b82f6; --accent2:#1d4ed8;
                --green:#2ea043; --green-bg:#11331c; --pulse:#f59e0b;
              }
              *{box-sizing:border-box}
              body{margin:0;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",
                "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif;
                background:var(--bg);color:var(--txt);font-size:14px;line-height:1.6}
              header{padding:18px 28px;border-bottom:1px solid var(--border);
                background:linear-gradient(180deg,#161b22,#0e1117)}
              header h1{margin:0;font-size:18px;font-weight:600;letter-spacing:.3px}
              header .sub{color:var(--muted);font-size:12px;margin-top:3px}
              .wrap{display:grid;grid-template-columns:300px 1fr;gap:18px;padding:18px 28px;
                align-items:start}
              .card{background:var(--panel);border:1px solid var(--border);border-radius:10px;
                padding:16px}
              .card h2{margin:0 0 12px;font-size:13px;font-weight:600;color:var(--muted);
                text-transform:uppercase;letter-spacing:.5px}
              .field{margin-bottom:14px}
              .field label{display:block;font-size:12px;color:var(--muted);margin-bottom:6px}
              .opt{display:flex;align-items:center;gap:8px;padding:7px 9px;border:1px solid var(--border);
                border-radius:7px;margin-bottom:6px;cursor:pointer;transition:.15s}
              .opt:hover{border-color:var(--accent)}
              .opt input{accent-color:var(--accent)}
              .opt.dis{opacity:.45;cursor:not-allowed}
              input[type=text],input[type=range]{width:100%}
              input[type=text]{background:var(--panel2);border:1px solid var(--border);
                border-radius:7px;color:var(--txt);padding:8px 10px;font-size:14px}
              input[type=text]:focus{outline:none;border-color:var(--accent)}
              .paceval{float:right;color:var(--txt);font-variant-numeric:tabular-nums}
              button{width:100%;background:var(--accent);color:#fff;border:none;border-radius:8px;
                padding:11px;font-size:14px;font-weight:600;cursor:pointer;transition:.15s}
              button:hover{background:var(--accent2)}
              button:disabled{opacity:.5;cursor:not-allowed}
              .right{display:flex;flex-direction:column;gap:18px;min-width:0}
              .pipe-head{display:flex;justify-content:space-between;align-items:baseline;
                margin-bottom:12px}
              .pipe-head .count{font-size:12px;color:var(--muted);font-variant-numeric:tabular-nums}
              .chips{display:flex;flex-wrap:wrap;gap:8px}
              .chip{display:flex;align-items:center;gap:6px;background:var(--panel2);
                border:1px solid var(--border);border-radius:20px;padding:6px 13px;font-size:12.5px;
                color:var(--muted);transition:.2s}
              .chip .dot{width:7px;height:7px;border-radius:50%;background:var(--border)}
              .chip.running{color:var(--txt);border-color:var(--pulse);
                animation:pulse 1s ease-in-out infinite}
              .chip.running .dot{background:var(--pulse)}
              .chip.done{color:var(--green);border-color:var(--green);background:var(--green-bg)}
              .chip.done .dot{background:var(--green)}
              .chip.done .dot::after{content:"";}
              @keyframes pulse{0%,100%{box-shadow:0 0 0 0 rgba(245,158,11,.4)}
                50%{box-shadow:0 0 0 5px rgba(245,158,11,0)}}
              .badges{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:14px}
              .badge{font-size:12px;padding:5px 11px;border-radius:7px;background:var(--panel2);
                border:1px solid var(--border)}
              .badge b{color:var(--accent)}
              .badge.warn b{color:var(--pulse)}
              .preview{background:#0d1117;border:1px solid var(--border);border-radius:8px;
                padding:22px 26px;max-height:62vh;overflow:auto}
              .preview h1{font-size:22px;border-bottom:1px solid var(--border);padding-bottom:8px}
              .preview h2{font-size:17px;margin-top:24px;color:var(--accent)}
              .preview h3{font-size:14px;color:var(--muted)}
              .preview hr{border:none;border-top:1px solid var(--border);margin:18px 0}
              .preview blockquote{margin:14px 0;padding:10px 16px;border-left:3px solid var(--accent);
                background:var(--panel2);border-radius:0 6px 6px 0;color:var(--txt)}
              .preview ul{padding-left:22px}
              .preview p{color:#c9d1d9}
              .empty{color:var(--muted);text-align:center;padding:50px 0}
              .note{font-size:12px;color:var(--pulse);margin-top:8px}
            </style>
            </head>
            <body>
            <header>
              <h1>研报生成 · 多智能体引擎</h1>
              <div class="sub">9 个专家智能体 · 共享黑板协作 · 离线确定性脚本模型演示</div>
            </header>
            <div class="wrap">
              <!-- LEFT: config -->
              <div class="card">
                <h2>配置</h2>
                <div class="field">
                  <label>报告类型</label>
                  <label class="opt"><input type="radio" name="type" value="equity" checked/> 个股研报</label>
                </div>
                <div class="field">
                  <label>数据源</label>
                  <label class="opt"><input type="radio" name="source" value="tushare" checked/> Tushare(缺省)</label>
                  <label class="opt"><input type="radio" name="source" value="wind"/> Wind</label>
                  <label class="opt"><input type="radio" name="source" value="choice"/> 东方财富 Choice</label>
                  <label class="opt"><input type="radio" name="source" value="stub"/> 桩(离线)</label>
                </div>
                <div class="field">
                  <label>标的代码</label>
                  <input type="text" id="ticker" value="DEMO" autocomplete="off"/>
                </div>
                <div class="field">
                  <label>演示节奏 <span class="paceval" id="paceval">250 ms</span></label>
                  <input type="range" id="pace" min="0" max="1000" step="50" value="250"/>
                </div>
                <button id="go">生成研报</button>
                <div class="note" id="note"></div>
              </div>

              <!-- RIGHT: pipeline + preview -->
              <div class="right">
                <div class="card">
                  <div class="pipe-head">
                    <h2 style="margin:0">智能体流水线</h2>
                    <span class="count" id="count">运行中 0 · 完成 0 / 9</span>
                  </div>
                  <div class="chips" id="chips"></div>
                </div>
                <div class="card">
                  <h2>报告预览</h2>
                  <div class="badges" id="badges"></div>
                  <div class="preview" id="preview"><div class="empty">点击「生成研报」开始 ——</div></div>
                </div>
              </div>
            </div>

            <script>
            (function(){
              var AGENTS=[
                ["planner","规划"],["data","数据"],["quant-model","建模"],
                ["valuation","估值"],["sector-macro","行业宏观"],["lead-manager","首席"],
                ["writer","撰写"],["critic","评审"],["compliance","合规"]
              ];
              var chipEl={};
              function buildChips(){
                var c=document.getElementById('chips'); c.innerHTML='';
                chipEl={};
                AGENTS.forEach(function(a){
                  var d=document.createElement('div');
                  d.className='chip'; d.id='chip-'+a[0];
                  d.innerHTML='<span class="dot"></span>'+a[1];
                  c.appendChild(d); chipEl[a[0]]=d;
                });
              }
              function recount(){
                var running=0,done=0;
                AGENTS.forEach(function(a){
                  var cl=chipEl[a[0]].className;
                  if(cl.indexOf('done')>=0) done++;
                  else if(cl.indexOf('running')>=0) running++;
                });
                document.getElementById('count').textContent=
                  '运行中 '+running+' · 完成 '+done+' / 9';
              }
              var pace=document.getElementById('pace');
              var paceval=document.getElementById('paceval');
              pace.addEventListener('input',function(){paceval.textContent=pace.value+' ms';});

              var go=document.getElementById('go');
              var es=null;
              go.addEventListener('click',function(){
                if(es){es.close();}
                buildChips(); recount();
                document.getElementById('badges').innerHTML='';
                document.getElementById('note').textContent='';
                document.getElementById('preview').innerHTML=
                  '<div class="empty">流水线运行中 ——</div>';
                go.disabled=true; go.textContent='生成中…';

                var type=document.querySelector('input[name=type]:checked').value;
                var source=document.querySelector('input[name=source]:checked').value;
                var ticker=encodeURIComponent(document.getElementById('ticker').value||'DEMO');
                var p=pace.value;
                es=new EventSource('/api/run?type='+type+'&source='+source+
                  '&ticker='+ticker+'&pace='+p);

                es.addEventListener('agent',function(e){
                  var d=JSON.parse(e.data);
                  var el=chipEl[d.role];
                  if(!el) return;
                  el.classList.remove('running','done');
                  el.classList.add(d.state==='done'?'done':'running');
                  recount();
                });
                es.addEventListener('note',function(e){
                  document.getElementById('note').textContent=JSON.parse(e.data).message||'';
                });
                es.addEventListener('report',function(e){
                  var d=JSON.parse(e.data);
                  var pt=(d.priceTarget!=null)?Number(d.priceTarget).toFixed(2):'-';
                  var up=(d.upsidePct!=null)?(Number(d.upsidePct)*100).toFixed(1)+'%':'-';
                  var degClass=(d.degradations>0)?'badge warn':'badge';
                  document.getElementById('badges').innerHTML=
                    '<span class="badge">评级 <b>'+esc(d.rating)+'</b></span>'+
                    '<span class="badge">目标价 <b>'+pt+'</b></span>'+
                    '<span class="badge">潜在空间 <b>'+up+'</b></span>'+
                    '<span class="badge">模型调用 <b>'+d.modelCalls+'</b></span>'+
                    '<span class="badge">改稿轮数 <b>'+d.criticRounds+'</b></span>'+
                    '<span class="'+degClass+'">降级 <b>'+d.degradations+'</b></span>';
                  document.getElementById('preview').innerHTML=d.html;
                });
                es.addEventListener('error',function(e){
                  var msg='连接中断';
                  try{ if(e.data){ msg=JSON.parse(e.data).message||msg; } }catch(_){}
                  document.getElementById('note').textContent='出错:'+msg;
                  finish();
                });
                es.addEventListener('done',function(){ finish(); });
              });
              function finish(){
                if(es){es.close();es=null;}
                go.disabled=false; go.textContent='生成研报';
              }
              function esc(s){
                return String(s==null?'':s).replace(/&/g,'&amp;')
                  .replace(/</g,'&lt;').replace(/>/g,'&gt;');
              }
              buildChips();
            })();
            </script>
            </body>
            </html>
            """;
}
