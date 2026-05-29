package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.monitor.MemoryHistoryRecorder;
import org.example.service.JvmMonitorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * JVM 监控控制器
 * 提供堆内存、线程（虚拟线程/平台线程）、GC 等实时监控 API
 */
@RestController
@RequestMapping("/api/monitor/jvm")
@RequiredArgsConstructor
public class JvmMonitorController {

    private final JvmMonitorService monitorService;
    private final MemoryHistoryRecorder memoryHistoryRecorder;

    /**
     * JVM 综合概览 - 一次调用获取全部关键指标（含 GC 告警）
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        JvmMonitorService.JvmOverview data = monitorService.getOverview();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 内存详情 - 堆/非堆内存 + 各内存池 + 物理内存
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> memory() {
        JvmMonitorService.MemoryDetail data = monitorService.getMemoryDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 线程详情 - 虚拟线程 vs 平台线程统计 + CPU Top 线程 + 状态分布
     */
    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> threads() {
        JvmMonitorService.ThreadDetail data = monitorService.getThreadDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 线程转储 - 所有线程及堆栈跟踪
     */
    @GetMapping("/thread-dump")
    public ResponseEntity<Map<String, Object>> threadDump() {
        JvmMonitorService.ThreadDump data = monitorService.getThreadDump();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * GC 详情 - 各 GC 收集器详情、最近 GC 内存变化、异常告警
     */
    @GetMapping("/gc")
    public ResponseEntity<Map<String, Object>> gc() {
        JvmMonitorService.GcDetailResult data = monitorService.getGcDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * GC 事件历史 - 最近 GC 事件时间线 + Young/Full GC 分类统计（含暂停时间分布）
     */
    @GetMapping("/gc/history")
    public ResponseEntity<Map<String, Object>> gcHistory() {
        JvmMonitorService.GcHistory data = monitorService.getGcHistory();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 堆内存历史数据 - 时间序列采样点，用于绘制内存变化曲线图
     * @param seconds 回溯时间范围（秒），默认 300（5 分钟），最大 1800（30 分钟）
     */
    @GetMapping("/memory/history")
    public ResponseEntity<Map<String, Object>> memoryHistory(
            @RequestParam(defaultValue = "300") int seconds) {
        int safeSeconds = Math.min(Math.max(seconds, 10), 1800);
        List<MemoryHistoryRecorder.MemorySample> samples = memoryHistoryRecorder.getHistory(safeSeconds);

        // 将 record 转为 Map 列表以便 JSON 序列化字段名友好
        List<Map<String, Object>> points = samples.stream()
                .map(s -> Map.<String, Object>of(
                        "timestamp", s.timestamp(),
                        "heapUsed", s.heapUsed(),
                        "heapMax", s.heapMax(),
                        "heapCommitted", s.heapCommitted(),
                        "heapUsagePercent", Math.round(s.heapUsagePercent() * 100.0) / 100.0,
                        "nonHeapUsed", s.nonHeapUsed()
                )).toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "samples", points,
                        "sampleCount", points.size(),
                        "totalSamples", memoryHistoryRecorder.getSampleCount(),
                        "intervalSeconds", memoryHistoryRecorder.getSampleIntervalSeconds(),
                        "querySeconds", safeSeconds
                ),
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 堆内存实时曲线图 — 返回 ECharts 可视化页面（HTML）
     */
    @GetMapping(value = "/memory/chart", produces = MediaType.TEXT_HTML_VALUE)
    public String memoryChart() {
        return MEMORY_CHART_HTML;
    }

    private static final String MEMORY_CHART_HTML = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>JVM 堆内存实时曲线</title>
            <script src="https://cdn.jsdelivr.net/npm/echarts@5.6.0/dist/echarts.min.js"></script>
            <style>
              * { margin: 0; padding: 0; box-sizing: border-box; }
              body { background: #1a1a2e; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
              .dashboard { display: flex; flex-wrap: wrap; padding: 16px; gap: 16px; }
              .card { background: #16213e; border-radius: 8px; padding: 16px; flex: 1; min-width: 360px; box-shadow: 0 2px 8px rgba(0,0,0,.3); }
              .card h3 { color: #e0e0e0; font-size: 14px; margin-bottom: 8px; font-weight: 500; }
              .chart { width: 100%; height: 360px; }
              .stats { display: flex; gap: 24px; flex-wrap: wrap; }
              .stat { color: #a0a0a0; font-size: 12px; }
              .stat strong { display: block; color: #e0e0e0; font-size: 20px; font-weight: 600; }
              .legend { display: flex; gap: 16px; margin-bottom: 8px; font-size: 11px; }
              .legend span { display: inline-flex; align-items: center; gap: 4px; color: #a0a0a0; }
              .legend i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; }
              .bar { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; font-size: 11px; color: #a0a0a0; }
              .bar-fill { height: 6px; border-radius: 3px; transition: width .3s; }
            </style>
            </head>
            <body>
            <div class="dashboard">
              <div class="card" style="flex:2; min-width:600px;">
                <h3>堆内存使用趋势 (Heap Memory)</h3>
                <div class="legend">
                  <span><i style="background:#4fc3f7;"></i> 已使用 (Used)</span>
                  <span><i style="background:#81c784;"></i> 已提交 (Committed)</span>
                  <span><i style="background:#e57373;"></i> 最大 (Max)</span>
                </div>
                <div id="heapChart" class="chart"></div>
              </div>
              <div class="card">
                <h3>实时指标</h3>
                <div class="stats" id="statsPanel"></div>
                <div style="margin-top:16px;">
                  <h3 style="margin-bottom:8px;">堆使用率</h3>
                  <div class="bar"><span>Heap</span><span id="heapPercent" style="flex:1; text-align:right;">--</span></div>
                  <div class="bar-fill" id="heapBar" style="width:0; background:#4fc3f7;"></div>
                </div>
                <div style="margin-top:14px;">
                  <h3 style="margin-bottom:8px;">各内存池</h3>
                  <div id="poolBars"></div>
                </div>
              </div>
            </div>
            <script>
              const heapChart = echarts.init(document.getElementById('heapChart'));
              const REFRESH_MS = 5000;

              // 从 URL query 或 localStorage 读取 token
              const params = new URLSearchParams(window.location.search);
              const token = params.get('token') || localStorage.getItem('jvm_monitor_token') || '';
              if (token) {
                localStorage.setItem('jvm_monitor_token', token);
                // 清除 URL 中的 token，避免泄露在浏览器历史中
                if (params.has('token')) {
                  const url = new URL(window.location);
                  url.searchParams.delete('token');
                  window.history.replaceState({}, '', url);
                }
              }
              const AUTH_HEADER = token ? { 'Authorization': 'Bearer ' + token } : {};

              function formatBytes(bytes) {
                if (!bytes || bytes <= 0) return '0 B';
                const units = ['B', 'KB', 'MB', 'GB'];
                const i = Math.floor(Math.log(bytes) / Math.log(1024));
                return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
              }

              function renderChart(samples) {
                const ts = samples.map(s => {
                  const d = new Date(s.timestamp);
                  return d.getHours().toString().padStart(2,'0') + ':'
                       + d.getMinutes().toString().padStart(2,'0') + ':'
                       + d.getSeconds().toString().padStart(2,'0');
                });
                heapChart.setOption({
                  tooltip: {
                    trigger: 'axis',
                    valueFormatter: v => formatBytes(v),
                    backgroundColor: 'rgba(0,0,0,.7)',
                    borderColor: '#333',
                    textStyle: { color: '#e0e0e0', fontSize: 12 }
                  },
                  grid: { left: 70, right: 20, top: 10, bottom: 30 },
                  xAxis: {
                    type: 'category', data: ts,
                    axisLine: { lineStyle: { color: '#444' } },
                    axisLabel: { color: '#888', fontSize: 10, rotate: ts.length > 30 ? 45 : 0 }
                  },
                  yAxis: {
                    type: 'value',
                    axisLabel: { color: '#888', fontSize: 10, formatter: v => formatBytes(v) },
                    splitLine: { lineStyle: { color: '#2a2a3a' } }
                  },
                  series: [
                    {
                      name: '已使用', type: 'line', data: samples.map(s => s.heapUsed),
                      smooth: true, symbol: 'none',
                      lineStyle: { color: '#4fc3f7', width: 2 },
                      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
                        [{offset:0, color:'rgba(79,195,247,.3)'},{offset:1, color:'rgba(79,195,247,.02)'}]) }
                    },
                    {
                      name: '已提交', type: 'line', data: samples.map(s => s.heapCommitted),
                      smooth: true, symbol: 'none',
                      lineStyle: { color: '#81c784', width: 1.5, type: 'dashed' }
                    },
                    {
                      name: '最大', type: 'line', data: samples.map(s => s.heapMax),
                      smooth: true, symbol: 'none',
                      lineStyle: { color: '#e57373', width: 1, type: 'dotted' }
                    }
                  ]
                });
              }

              function renderStats(samples, pools) {
                if (!samples.length) return;
                const last = samples[samples.length - 1];
                document.getElementById('statsPanel').innerHTML = [
                  ['堆已用', formatBytes(last.heapUsed)],
                  ['堆已提交', formatBytes(last.heapCommitted)],
                  ['堆最大', formatBytes(last.heapMax)],
                  ['使用率', last.heapUsagePercent.toFixed(1) + '%'],
                  ['非堆', formatBytes(last.nonHeapUsed)],
                  ['样本数', samples.length]
                ].map(([k,v]) => '<div class="stat"><strong>'+v+'</strong>'+k+'</div>').join('');

                const pct = last.heapUsagePercent.toFixed(1);
                document.getElementById('heapPercent').textContent = pct + '%';
                document.getElementById('heapBar').style.width = Math.min(Number(pct), 100) + '%';
                document.getElementById('heapBar').style.background = Number(pct) > 85 ? '#e57373' : '#4fc3f7';

                if (pools && pools.length) {
                  document.getElementById('poolBars').innerHTML = pools.map(p =>
                    '<div style="margin-bottom:6px;">' +
                    '<div class="bar"><span>'+p.name+'</span><span style="flex:1;text-align:right;">'+
                    formatBytes(p.used)+' / '+formatBytes(p.max>0?p.max:p.committed)+
                    ' ('+p.usagePercent.toFixed(1)+'%)</span></div>' +
                    '<div class="bar-fill" style="width:'+Math.min(p.usagePercent,100)+'%;'+
                    'background:'+(p.usagePercent>85?'#e57373':'#4fc3f7')+';"></div></div>'
                  ).join('');
                }
              }

              async function refresh() {
                try {
                  const [histResp, memResp] = await Promise.all([
                    fetch('/api/monitor/jvm/memory/history?seconds=900', { headers: AUTH_HEADER }),
                    fetch('/api/monitor/jvm/memory', { headers: AUTH_HEADER })
                  ]);
                  const hist = await histResp.json();
                  const mem = await memResp.json();
                  if (hist.success && hist.data.samples.length) {
                    renderChart(hist.data.samples);
                    renderStats(hist.data.samples, mem.success ? mem.data.summary.pools : null);
                  }
                } catch (e) { console.error('Refresh error:', e); }
              }

              refresh();
              setInterval(refresh, REFRESH_MS);
              window.addEventListener('resize', () => heapChart.resize());
            </script>
            </body>
            </html>
            """;
}
