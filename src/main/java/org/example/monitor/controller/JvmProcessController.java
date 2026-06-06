package org.example.monitor.controller;

import org.example.monitor.service.JvmProcessService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor/jvm/processes")
public class JvmProcessController {

    private final JvmProcessService jvmProcessService;

    public JvmProcessController(JvmProcessService jvmProcessService) {
        this.jvmProcessService = jvmProcessService;
    }

    @GetMapping
    public Map<String, Object> listProcesses() {
        return Map.of(
                "success", true,
                "data", jvmProcessService.listProcesses(),
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/{pid}")
    public Map<String, Object> getProcessDetail(@PathVariable String pid) {
        return Map.of(
                "success", true,
                "data", jvmProcessService.getProcessDetail(pid),
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/{pid}/gc")
    public Map<String, Object> getProcessGc(@PathVariable String pid) {
        var detail = jvmProcessService.getProcessDetail(pid);
        return Map.of(
                "success", true,
                "data", Map.of("pid", pid, "gcStats", detail.gcStats(), "heapPools", detail.heapPools()),
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/{pid}/thread-dump")
    public Map<String, Object> getThreadDump(@PathVariable String pid) {
        return Map.of(
                "success", true,
                "data", jvmProcessService.getThreadDump(pid),
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping(value = "/chart", produces = MediaType.TEXT_HTML_VALUE)
    public String chart() {
        return CHART_HTML;
    }

    private static final String CHART_HTML = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>服务器 JVM 进程监控</title>
            <script src="https://cdn.jsdelivr.net/npm/echarts@5.6.0/dist/echarts.min.js"></script>
            <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { background: #1a1a2e; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color: #e0e0e0; padding: 16px; }
            .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
            .header h1 { font-size: 20px; color: #4fc3f7; }
            .header span { font-size: 12px; color: #888; }
            .process-table { background: #16213e; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.3); }
            .process-table h3 { font-size: 14px; color: #a0a0a0; margin-bottom: 12px; }
            table { width: 100%; border-collapse: collapse; font-size: 13px; }
            th { text-align: left; padding: 8px 12px; border-bottom: 1px solid #2a2a4a; color: #888; font-weight: 500; }
            td { padding: 8px 12px; border-bottom: 1px solid #1a1a2e; }
            tr:hover td { background: rgba(79,195,247,.05); }
            .pid { color: #4fc3f7; font-family: monospace; cursor: pointer; }
            .pid:hover { text-decoration: underline; }
            .charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
            .chart-card { background: #16213e; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.3); }
            .chart-card h3 { font-size: 14px; color: #a0a0a0; margin-bottom: 8px; }
            .chart { width: 100%; height: 360px; }
            </style>
            </head>
            <body>
            <div class="header">
              <h1>🖥 服务器 JVM 进程监控</h1>
              <span id="refreshInfo">上次刷新: -- | 自动刷新: 5秒</span>
            </div>
            <div class="process-table" id="processTable">
              <h3>Java 进程列表</h3>
              <table>
                <thead><tr><th>PID</th><th>主类</th><th>堆已用</th><th>堆最大</th><th>使用率</th><th>运行时长</th><th>JVM 版本</th></tr></thead>
                <tbody id="tableBody"><tr><td colspan="7" style="text-align:center;color:#888;">加载中...</td></tr></tbody>
              </table>
            </div>
            <div class="charts-row">
              <div class="chart-card"><h3>堆内存使用对比</h3><div class="chart" id="heapChart"></div></div>
              <div class="chart-card"><h3>GC 统计对比</h3><div class="chart" id="gcChart"></div></div>
            </div>
            <div class="charts-row">
              <div class="chart-card"><h3>堆使用率饼图</h3><div class="chart" id="usageChart"></div></div>
              <div class="chart-card"><h3>进程运行时长</h3><div class="chart" id="uptimeChart"></div></div>
            </div>
            <script>
            const REFRESH_MS = 5000;
            const params = new URLSearchParams(window.location.search);
            const token = params.get('token') || localStorage.getItem('jvm_processes_token') || '';
            if (token) {
              localStorage.setItem('jvm_processes_token', token);
              if (params.has('token')) {
                const url = new URL(window.location);
                url.searchParams.delete('token');
                window.history.replaceState({}, '', url);
              }
            }
            const AUTH_HEADER = token ? { 'Authorization': 'Bearer ' + token } : {};

            const heapChart = echarts.init(document.getElementById('heapChart'));
            const gcChart = echarts.init(document.getElementById('gcChart'));
            const usageChart = echarts.init(document.getElementById('usageChart'));
            const uptimeChart = echarts.init(document.getElementById('uptimeChart'));

            function formatBytes(b) {
              if (b < 0) return 'N/A';
              if (b < 1024) return b + ' B';
              if (b < 1048576) return (b / 1024).toFixed(1) + ' KB';
              if (b < 1073741824) return (b / 1048576).toFixed(1) + ' MB';
              return (b / 1073741824).toFixed(2) + ' GB';
            }

            function formatPct(n) { return typeof n === 'number' ? n.toFixed(1) + '%' : '--'; }

            async function refresh() {
              try {
                const resp = await fetch('/api/monitor/jvm/processes', { headers: AUTH_HEADER });
                const json = await resp.json();
                if (!json.success || !json.data) return;
                const procs = json.data;

                renderTable(procs);
                renderHeapChart(procs);
                renderGcChart(procs);
                renderUsageChart(procs);
                renderUptimeChart(procs);
                document.getElementById('refreshInfo').textContent =
                  '上次刷新: ' + new Date().toLocaleTimeString() + ' | 自动刷新: ' + (REFRESH_MS / 1000) + '秒';
              } catch (e) {
                console.error('刷新失败:', e);
              }
            }

            function renderTable(procs) {
              const tbody = document.getElementById('tableBody');
              if (procs.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#888;">无 Java 进程</td></tr>';
                return;
              }
              tbody.innerHTML = procs.map(p => {
                const pct = p.heapMaxBytes > 0 ? (p.heapUsedBytes / p.heapMaxBytes * 100).toFixed(1) : 0;
                return '<tr>' +
                  '<td><a class="pid" href="/api/monitor/jvm/processes/' + p.pid + '" target="_blank">' + p.pid + '</a></td>' +
                  '<td title="' + (p.jvmArgs || '').replace(/"/g, '&quot;') + '">' + p.mainClass + '</td>' +
                  '<td>' + formatBytes(p.heapUsedBytes) + '</td>' +
                  '<td>' + formatBytes(p.heapMaxBytes) + '</td>' +
                  '<td>' + pct + '%</td>' +
                  '<td>' + p.uptime + '</td>' +
                  '<td>' + p.jvmVersion + '</td>' +
                  '</tr>';
              }).join('');
            }

            function renderHeapChart(procs) {
              const names = procs.map(p => p.pid + ' ' + p.mainClass.substring(p.mainClass.lastIndexOf('.') + 1));
              const usedVals = procs.map(p => p.heapUsedBytes);
              const maxVals = procs.map(p => p.heapMaxBytes > 0 ? p.heapMaxBytes : null);
              heapChart.setOption({
                tooltip: { trigger: 'axis', backgroundColor: 'rgba(0,0,0,.7)', borderColor: '#333',
                  textStyle: { color: '#e0e0e0', fontSize: 12 },
                  valueFormatter: v => formatBytes(v) },
                legend: { data: ['堆已用', '堆最大'], textStyle: { color: '#888' }, top: 0 },
                grid: { left: 60, right: 20, top: 30, bottom: 80 },
                xAxis: { type: 'category', data: names, axisLabel: { color: '#888', fontSize: 11, rotate: 30 } },
                yAxis: { type: 'value', axisLabel: { color: '#888', formatter: v => formatBytes(v) },
                  splitLine: { lineStyle: { color: '#2a2a3a' } } },
                series: [
                  { name: '堆已用', type: 'bar', data: usedVals, itemStyle: { color: '#4fc3f7' }, barMaxWidth: 40 },
                  { name: '堆最大', type: 'bar', data: maxVals, itemStyle: { color: '#5c6bc0' }, barMaxWidth: 40 }
                ]
              }, true);
            }

            function renderGcChart(procs) {
              // Show GC data from detail — for chart view aggregate from processes endpoint won't have it
              // Show placeholder with process list
              var names = procs.map(p => p.pid);
              var ygcVals = [];
              var fgcVals = [];
              // We can't get detailed GC from list endpoint, show simple comparison
              Promise.all(procs.map(p =>
                fetch('/api/monitor/jvm/processes/' + p.pid + '/gc', { headers: AUTH_HEADER })
                  .then(r => r.json())
                  .then(j => j.success && j.data ? j.data.gcStats : null)
                  .catch(() => null)
              )).then(results => {
                names = procs.map((p, i) => p.pid + ' ' + p.mainClass.substring(p.mainClass.lastIndexOf('.') + 1));
                ygcVals = results.map(r => r ? r.youngGcCount : 0);
                fgcVals = results.map(r => r ? r.fullGcCount : 0);
                gcChart.setOption({
                  tooltip: { trigger: 'axis', backgroundColor: 'rgba(0,0,0,.7)', borderColor: '#333',
                    textStyle: { color: '#e0e0e0', fontSize: 12 } },
                  legend: { data: ['Young GC', 'Full GC'], textStyle: { color: '#888' }, top: 0 },
                  grid: { left: 60, right: 20, top: 30, bottom: 80 },
                  xAxis: { type: 'category', data: names, axisLabel: { color: '#888', fontSize: 11, rotate: 30 } },
                  yAxis: { type: 'value', axisLabel: { color: '#888' }, splitLine: { lineStyle: { color: '#2a2a3a' } } },
                  series: [
                    { name: 'Young GC', type: 'bar', data: ygcVals, itemStyle: { color: '#81c784' }, barMaxWidth: 35 },
                    { name: 'Full GC', type: 'bar', data: fgcVals, itemStyle: { color: '#e57373' }, barMaxWidth: 35 }
                  ]
                }, true);
              });
            }

            function renderUsageChart(procs) {
              var data = procs.map(p => {
                var pct = p.heapMaxBytes > 0 ? (p.heapUsedBytes / p.heapMaxBytes * 100) : 0;
                return { name: p.pid + ' ' + p.mainClass.substring(p.mainClass.lastIndexOf('.') + 1), value: parseFloat(pct.toFixed(1)) };
              });
              usageChart.setOption({
                tooltip: { trigger: 'item', backgroundColor: 'rgba(0,0,0,.7)', borderColor: '#333',
                  textStyle: { color: '#e0e0e0', fontSize: 12 }, formatter: '{b}: {c}%' },
                series: [{
                  type: 'pie', radius: ['45%', '75%'], center: ['50%', '55%'],
                  label: { color: '#888', fontSize: 11 }, data: data,
                  itemStyle: { borderColor: '#1a1a2e', borderWidth: 2 }
                }]
              }, true);
            }

            function renderUptimeChart(procs) {
              var names = procs.map(p => p.pid);
              var vals = procs.map(p => (p.uptimeMs / 3600000).toFixed(1));
              uptimeChart.setOption({
                tooltip: { trigger: 'axis', backgroundColor: 'rgba(0,0,0,.7)', borderColor: '#333',
                  textStyle: { color: '#e0e0e0', fontSize: 12 }, valueFormatter: v => v + ' 小时' },
                grid: { left: 60, right: 20, top: 20, bottom: 80 },
                xAxis: { type: 'category', data: names, axisLabel: { color: '#888', fontSize: 11, rotate: 30 } },
                yAxis: { type: 'value', axisLabel: { color: '#888', formatter: v => v + 'h' },
                  splitLine: { lineStyle: { color: '#2a2a3a' } } },
                series: [{
                  type: 'bar', data: vals, itemStyle: { color: '#ffb74d' }, barMaxWidth: 60,
                  label: { show: true, position: 'top', color: '#888', fontSize: 11, formatter: '{c}h' }
                }]
              }, true);
            }

            refresh();
            setInterval(refresh, REFRESH_MS);
            window.addEventListener('resize', () => {
              heapChart.resize(); gcChart.resize(); usageChart.resize(); uptimeChart.resize();
            });
            </script>
            </body>
            </html>
            """;
}
