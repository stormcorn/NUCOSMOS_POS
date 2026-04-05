# `.codex-portable` 同步清單

這份清單用來把目前這台電腦的 Codex 可攜設定，安全同步到另一台電腦的 VS Code / Codex 環境。

## 目的

- 同步 Codex 自訂規則
- 同步 Codex skills
- 同步 Codex memories
- 保留一致的 `config.toml`
- 避免把登入憑證、本機資料庫、暫存檔一起搬過去

## 建議同步方式

### 1. 專案程式碼

用 `git` 同步：

```powershell
git clone https://github.com/stormcorn/NUCOSMOS_POS.git
```

### 2. VS Code 設定

用 VS Code 內建 `Settings Sync`。

### 3. Codex 可攜資料

來源路徑：

```text
C:\Users\<你的帳號>\.codex
```

目標路徑：

```text
C:\Users\<另一台電腦的帳號>\.codex
```

## 要同步

以下檔案與資料夾可以安全同步：

- `config.toml`
- `rules/`
- `skills/`
- `memories/`

## 不要同步

以下內容不要直接搬到另一台：

- `auth.json`
- `sessions/`
- `sqlite/`
- `cache/`
- `tmp/`
- `.tmp/`
- `.sandbox/`
- `.sandbox-bin/`
- `.sandbox-secrets/`
- `vendor_imports/`
- `logs_1.sqlite`
- `logs_1.sqlite-shm`
- `logs_1.sqlite-wal`
- `state_5.sqlite`
- `state_5.sqlite-shm`
- `state_5.sqlite-wal`
- `session_index.jsonl`
- `models_cache.json`
- `cap_sid`
- `.codex-global-state.json`

## 建議複製清單

### 來源電腦要打包的項目

```text
%USERPROFILE%\.codex\config.toml
%USERPROFILE%\.codex\rules
%USERPROFILE%\.codex\skills
%USERPROFILE%\.codex\memories
```

### 另一台電腦要自行處理的項目

- 重新安裝 VS Code
- 重新安裝 Codex
- 重新登入 Codex
- 重新啟用 VS Code `Settings Sync`

## PowerShell 複製範例

先在來源電腦整理出一份可攜資料夾：

```powershell
$portable = "$HOME\\Desktop\\codex-portable"
New-Item -ItemType Directory -Force -Path $portable | Out-Null
Copy-Item "$HOME\\.codex\\config.toml" $portable -Force
Copy-Item "$HOME\\.codex\\rules" $portable -Recurse -Force
Copy-Item "$HOME\\.codex\\skills" $portable -Recurse -Force
Copy-Item "$HOME\\.codex\\memories" $portable -Recurse -Force
```

在另一台電腦還原：

```powershell
$portable = "$HOME\\Desktop\\codex-portable"
New-Item -ItemType Directory -Force -Path "$HOME\\.codex" | Out-Null
Copy-Item "$portable\\config.toml" "$HOME\\.codex" -Force
Copy-Item "$portable\\rules" "$HOME\\.codex" -Recurse -Force
Copy-Item "$portable\\skills" "$HOME\\.codex" -Recurse -Force
Copy-Item "$portable\\memories" "$HOME\\.codex" -Recurse -Force
```

## 還原後檢查

在另一台電腦確認：

- `~/.codex/config.toml` 已存在
- `~/.codex/rules` 已存在
- `~/.codex/skills` 已存在
- `~/.codex/memories` 已存在
- Codex 已重新登入
- 專案 repo 已 `git clone`

## 這份清單的原則

- 程式碼靠 `git`
- 編輯器偏好靠 VS Code `Settings Sync`
- Codex 只同步可攜的自訂設定
- 登入與暫存狀態不直接搬移
