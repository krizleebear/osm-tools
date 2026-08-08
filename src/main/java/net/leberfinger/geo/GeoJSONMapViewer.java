package net.leberfinger.geo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Generates an interactive, high-performance HTML map viewer for inspecting
 * original vs simplified/buffered GeoJSON datasets with admin level drill-down
 * filters, visual diff rendering, and browser overload prevention.
 */
public class GeoJSONMapViewer {

    public static Path generateViewer(Path inputFile, Path outputFile, Path htmlOutputFile) throws IOException {
        List<GeoJSON> inputFeatures = new ArrayList<>();
        if (inputFile != null && Files.exists(inputFile)) {
            try (Stream<GeoJSON> stream = GeoJSON.streamParsedGeoJSONLines(inputFile)) {
                stream.forEach(inputFeatures::add);
            }
        }

        List<GeoJSON> outputFeatures = new ArrayList<>();
        if (outputFile != null && Files.exists(outputFile)) {
            try (Stream<GeoJSON> stream = GeoJSON.streamParsedGeoJSONLines(outputFile)) {
                stream.forEach(outputFeatures::add);
            }
        }

        String htmlContent = buildHTML(inputFile != null ? inputFile.getFileName().toString() : "Dataset",
                inputFeatures, outputFeatures);

        try (BufferedWriter writer = Files.newBufferedWriter(htmlOutputFile)) {
            writer.write(htmlContent);
        }

        System.out.println("Map viewer generated successfully: " + htmlOutputFile.toAbsolutePath());
        return htmlOutputFile;
    }

    private static String buildHTML(String title, List<GeoJSON> inputFeatures, List<GeoJSON> outputFeatures) {
        StringBuilder inputJsonBuilder = new StringBuilder("[");
        appendFeaturesJSON(inputJsonBuilder, inputFeatures);
        inputJsonBuilder.append("]");

        StringBuilder outputJsonBuilder = new StringBuilder("[");
        appendFeaturesJSON(outputJsonBuilder, outputFeatures);
        outputJsonBuilder.append("]");

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>GeoJSON Simplification Map Viewer - " + escapeHTML(title) + "</title>\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        :root {\n" +
                "            --bg-dark: #0f172a;\n" +
                "            --panel-bg: rgba(30, 41, 59, 0.85);\n" +
                "            --panel-border: rgba(255, 255, 255, 0.12);\n" +
                "            --accent-input: #00f2fe;\n" +
                "            --accent-output: #10b981;\n" +
                "            --accent-diff: #f59e0b;\n" +
                "            --text-main: #f8fafc;\n" +
                "            --text-muted: #94a3b8;\n" +
                "        }\n" +
                "        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }\n" +
                "        body, html { width: 100%; height: 100%; overflow: hidden; background: var(--bg-dark); color: var(--text-main); }\n" +
                "        #map { width: 100%; height: 100%; background: #0b0f19; }\n" +
                "        \n" +
                "        /* Floating Glassmorphism Panels */\n" +
                "        .glass-panel {\n" +
                "            background: var(--panel-bg);\n" +
                "            backdrop-filter: blur(12px);\n" +
                "            -webkit-backdrop-filter: blur(12px);\n" +
                "            border: 1px solid var(--panel-border);\n" +
                "            border-radius: 12px;\n" +
                "            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);\n" +
                "            z-index: 1000;\n" +
                "        }\n" +
                "        .header-card {\n" +
                "            position: absolute; top: 16px; left: 16px; padding: 16px 20px;\n" +
                "            max-width: 420px; pointer-events: auto;\n" +
                "        }\n" +
                "        .header-title {\n" +
                "            font-size: 1.1rem; font-weight: 700; color: #fff;\n" +
                "            display: flex; align-items: center; gap: 8px; margin-bottom: 6px;\n" +
                "        }\n" +
                "        .header-subtitle { font-size: 0.8rem; color: var(--text-muted); line-height: 1.4; }\n" +
                "        \n" +
                "        .controls-card {\n" +
                "            position: absolute; top: 16px; right: 16px; padding: 16px;\n" +
                "            width: 340px; max-height: calc(100vh - 32px); overflow-y: auto;\n" +
                "        }\n" +
                "        .section-title {\n" +
                "            font-size: 0.75rem; font-weight: 700; text-transform: uppercase;\n" +
                "            letter-spacing: 0.05em; color: var(--text-muted); margin: 12px 0 6px 0;\n" +
                "        }\n" +
                "        .section-title:first-child { margin-top: 0; }\n" +
                "        \n" +
                "        .btn-group { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }\n" +
                "        .btn {\n" +
                "            background: rgba(255, 255, 255, 0.06); border: 1px solid rgba(255, 255, 255, 0.1);\n" +
                "            color: var(--text-main); padding: 6px 12px; font-size: 0.78rem; font-weight: 500;\n" +
                "            border-radius: 6px; cursor: pointer; transition: all 0.2s ease;\n" +
                "        }\n" +
                "        .btn:hover { background: rgba(255, 255, 255, 0.15); border-color: rgba(255, 255, 255, 0.3); }\n" +
                "        .btn.active { background: rgba(16, 185, 129, 0.25); border-color: #10b981; color: #34d399; font-weight: 600; }\n" +
                "        .btn-cyan.active { background: rgba(0, 242, 254, 0.25); border-color: #00f2fe; color: #38bdf8; }\n" +
                "        .btn-amber.active { background: rgba(245, 158, 11, 0.25); border-color: #f59e0b; color: #fbbf24; }\n" +
                "        \n" +
                "        .search-input {\n" +
                "            width: 100%; padding: 8px 12px; font-size: 0.8rem; border-radius: 6px;\n" +
                "            background: rgba(0,0,0,0.3); border: 1px solid var(--panel-border);\n" +
                "            color: #fff; outline: none; margin-bottom: 10px;\n" +
                "        }\n" +
                "        .search-input:focus { border-color: #38bdf8; }\n" +
                "        \n" +
                "        .stats-badge {\n" +
                "            display: flex; justify-content: space-between; align-items: center;\n" +
                "            padding: 8px 12px; background: rgba(0,0,0,0.25); border-radius: 6px;\n" +
                "            font-size: 0.78rem; margin-bottom: 6px;\n" +
                "        }\n" +
                "        .stats-label { color: var(--text-muted); }\n" +
                "        .stats-val { font-weight: 600; color: #fff; }\n" +
                "        \n" +
                "        .drop-zone {\n" +
                "            border: 2px dashed rgba(255, 255, 255, 0.2); border-radius: 8px;\n" +
                "            padding: 12px; text-align: center; font-size: 0.75rem; color: var(--text-muted);\n" +
                "            cursor: pointer; margin-top: 10px; transition: background 0.2s;\n" +
                "        }\n" +
                "        .drop-zone:hover { background: rgba(255, 255, 255, 0.05); border-color: #38bdf8; color: #fff; }\n" +
                "        \n" +
                "        /* Leaflet Dark Customization */\n" +
                "        .leaflet-container { background: #0b0f19 !important; }\n" +
                "        .leaflet-control-zoom a {\n" +
                "            background: var(--panel-bg) !important; color: #fff !important;\n" +
                "            border-color: var(--panel-border) !important;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    \n" +
                "    <div class=\"glass-panel header-card\">\n" +
                "        <div class=\"header-title\">🗺️ GeoJSON Simplification Viewer</div>\n" +
                "        <div class=\"header-subtitle\">" + escapeHTML(title) + " — Interactive drill-down & visual diff analysis</div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"glass-panel controls-card\">\n" +
                "        <div class=\"section-title\">Layer Mode</div>\n" +
                "        <div class=\"btn-group\">\n" +
                "            <button class=\"btn btn-cyan active\" id=\"btn-mode-input\" onclick=\"setMode('input')\">Input (Original)</button>\n" +
                "            <button class=\"btn active\" id=\"btn-mode-output\" onclick=\"setMode('output')\">Output (Simplified)</button>\n" +
                "            <button class=\"btn btn-amber active\" id=\"btn-mode-diff\" onclick=\"setMode('diff')\">Diff / Buffer Overlay</button>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"section-title\">Admin Level Drill-Down</div>\n" +
                "        <div class=\"btn-group\" id=\"admin-level-buttons\">\n" +
                "            <button class=\"btn active\" onclick=\"filterAdminLevel('ALL')\">ALL</button>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"section-title\">Feature Search</div>\n" +
                "        <input type=\"text\" class=\"search-input\" id=\"search-input\" placeholder=\"Search name or @id...\" oninput=\"onSearch(this.value)\">\n" +
                "        \n" +
                "        <div class=\"section-title\">Performance Guard</div>\n" +
                "        <div class=\"stats-badge\">\n" +
                "            <span class=\"stats-label\">Max Render Cap:</span>\n" +
                "            <span class=\"stats-val\" id=\"cap-label\">300 features</span>\n" +
                "        </div>\n" +
                "        <div class=\"stats-badge\">\n" +
                "            <span class=\"stats-label\">Rendered Features:</span>\n" +
                "            <span class=\"stats-val\" id=\"rendered-count\">0 / 0</span>\n" +
                "        </div>\n" +
                "        <div class=\"btn-group\" style=\"margin-top: 4px;\">\n" +
                "            <button class=\"btn\" onclick=\"setCap(100)\">100</button>\n" +
                "            <button class=\"btn active\" onclick=\"setCap(300)\">300</button>\n" +
                "            <button class=\"btn\" onclick=\"setCap(1000)\">1000</button>\n" +
                "            <button class=\"btn\" onclick=\"setCap(99999)\">UNLIMITED</button>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"drop-zone\" id=\"drop-zone\" onclick=\"document.getElementById('file-input').click()\">\n" +
                "            📁 Drag & Drop custom .geojsonseq file here\n" +
                "            <input type=\"file\" id=\"file-input\" style=\"display:none\" onchange=\"loadCustomFile(event)\">\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "    <script>\n" +
                "        const inputData = " + inputJsonBuilder.toString() + ";\n" +
                "        const outputData = " + outputJsonBuilder.toString() + ";\n" +
                "        \n" +
                "        let map = L.map('map', { renderer: L.canvas() }).setView([51.1657, 10.4515], 6);\n" +
                "        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {\n" +
                "            attribution: '&copy; OpenStreetMap &copy; CARTO',\n" +
                "            subdomains: 'abcd',\n" +
                "            maxZoom: 19\n" +
                "        }).addTo(map);\n" +
                "        \n" +
                "        let showInput = true;\n" +
                "        let showOutput = true;\n" +
                "        let showDiff = true;\n" +
                "        let selectedAdminLevel = 'ALL';\n" +
                "        let searchQuery = '';\n" +
                "        let maxRenderCap = 300;\n" +
                "        \n" +
                "        let inputLayerGroup = L.layerGroup().addTo(map);\n" +
                "        let outputLayerGroup = L.layerGroup().addTo(map);\n" +
                "        let diffLayerGroup = L.layerGroup().addTo(map);\n" +
                "        \n" +
                "        function extractAdminLevels() {\n" +
                "            const levels = new Set();\n" +
                "            [...inputData, ...outputData].forEach(f => {\n" +
                "                const p = f.properties || {};\n" +
                "                if (p.admin_level) levels.add('L' + p.admin_level);\n" +
                "                if (p.subtype) levels.add(p.subtype);\n" +
                "            });\n" +
                "            const container = document.getElementById('admin-level-buttons');\n" +
                "            container.innerHTML = '<button class=\"btn ' + (selectedAdminLevel==='ALL'?'active':'') + '\" onclick=\"filterAdminLevel(\\'ALL\\')\">ALL</button>';\n" +
                "            Array.from(levels).sort().forEach(lvl => {\n" +
                "                container.innerHTML += '<button class=\"btn ' + (selectedAdminLevel===lvl?'active':'') + '\" onclick=\"filterAdminLevel(\\'' + lvl + '\\')\">' + lvl + '</button>';\n" +
                "            });\n" +
                "        }\n" +
                "        \n" +
                "        function filterAdminLevel(lvl) {\n" +
                "            selectedAdminLevel = lvl;\n" +
                "            extractAdminLevels();\n" +
                "            renderMap();\n" +
                "        }\n" +
                "        \n" +
                "        function setMode(mode) {\n" +
                "            if (mode === 'input') showInput = !showInput;\n" +
                "            if (mode === 'output') showOutput = !showOutput;\n" +
                "            if (mode === 'diff') showDiff = !showDiff;\n" +
                "            \n" +
                "            document.getElementById('btn-mode-input').classList.toggle('active', showInput);\n" +
                "            document.getElementById('btn-mode-output').classList.toggle('active', showOutput);\n" +
                "            document.getElementById('btn-mode-diff').classList.toggle('active', showDiff);\n" +
                "            renderMap();\n" +
                "        }\n" +
                "        \n" +
                "        function setCap(cap) {\n" +
                "            maxRenderCap = cap;\n" +
                "            document.getElementById('cap-label').innerText = cap >= 99999 ? 'UNLIMITED' : cap + ' features';\n" +
                "            renderMap();\n" +
                "        }\n" +
                "        \n" +
                "        function onSearch(val) {\n" +
                "            searchQuery = val.toLowerCase().trim();\n" +
                "            renderMap();\n" +
                "        }\n" +
                "        \n" +
                "        function featureMatchesFilter(f) {\n" +
                "            const p = f.properties || {};\n" +
                "            if (selectedAdminLevel !== 'ALL') {\n" +
                "                const lvlStr = 'L' + p.admin_level;\n" +
                "                if (lvlStr !== selectedAdminLevel && p.subtype !== selectedAdminLevel) return false;\n" +
                "            }\n" +
                "            if (searchQuery) {\n" +
                "                const name = (p.name || '').toLowerCase();\n" +
                "                const id = String(p['@id'] || p.id || '').toLowerCase();\n" +
                "                if (!name.includes(searchQuery) && !id.includes(searchQuery)) return false;\n" +
                "            }\n" +
                "            return true;\n" +
                "        }\n" +
                "        \n" +
                "        function renderMap() {\n" +
                "            inputLayerGroup.clearLayers();\n" +
                "            outputLayerGroup.clearLayers();\n" +
                "            diffLayerGroup.clearLayers();\n" +
                "            \n" +
                "            let rendered = 0;\n" +
                "            let totalEligible = 0;\n" +
                "            const bounds = L.latLngBounds();\n" +
                "            \n" +
                "            const countToRender = Math.max(inputData.length, outputData.length);\n" +
                "            for (let i = 0; i < countToRender; i++) {\n" +
                "                const inF = inputData[i];\n" +
                "                const outF = outputData[i];\n" +
                "                const sampleF = inF || outF;\n" +
                "                if (!sampleF || !featureMatchesFilter(sampleF)) continue;\n" +
                "                \n" +
                "                totalEligible++;\n" +
                "                if (rendered >= maxRenderCap) continue;\n" +
                "                rendered++;\n" +
                "                \n" +
                "                if (showInput && inF && inF.geometry) {\n" +
                "                    const l = L.geoJSON(inF, {\n" +
                "                        style: { color: '#00f2fe', weight: 2, fillOpacity: 0.15 }\n" +
                "                    });\n" +
                "                    inputLayerGroup.addLayer(l);\n" +
                "                    bounds.extend(l.getBounds());\n" +
                "                }\n" +
                "                if (showOutput && outF && outF.geometry) {\n" +
                "                    const l = L.geoJSON(outF, {\n" +
                "                        style: { color: '#10b981', weight: 2.5, fillOpacity: 0.25 }\n" +
                "                    });\n" +
                "                    outputLayerGroup.addLayer(l);\n" +
                "                    bounds.extend(l.getBounds());\n" +
                "                }\n" +
                "                if (showDiff && outF && outF.geometry) {\n" +
                "                    const l = L.geoJSON(outF, {\n" +
                "                        style: { color: '#f59e0b', weight: 1, dashArray: '4,4', fillOpacity: 0.08 }\n" +
                "                    });\n" +
                "                    diffLayerGroup.addLayer(l);\n" +
                "                }\n" +
                "            }\n" +
                "            \n" +
                "            document.getElementById('rendered-count').innerText = rendered + ' / ' + totalEligible;\n" +
                "            if (rendered > 0 && bounds.isValid()) {\n" +
                "                map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        extractAdminLevels();\n" +
                "        renderMap();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private static void appendFeaturesJSON(StringBuilder sb, List<GeoJSON> features) {
        for (int i = 0; i < features.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(features.get(i).toJSON().toString());
        }
    }

    private static String escapeHTML(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java net.leberfinger.geo.GeoJSONMapViewer <inputFile.geojsonseq> <outputFile.geojsonseq> [htmlOutput.html]");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Path htmlOut = args.length >= 3 ? Path.of(args[2]) : Path.of("simplification_map_viewer.html");
        generateViewer(in, out, htmlOut);
    }
}
