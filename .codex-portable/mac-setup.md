# Windows -> Mac 的 Codex 同步方式

這份文件是給「來源電腦是 Windows、目標電腦是 Mac，而且兩邊都有 Codex」的情境。

## 結論先講

可以同步，但建議只同步可攜設定：

- `config.toml`
- `rules/`
- `skills/`
- `memories/`

不要直接同步：

- `auth.json`
- `sessions/`
- `sqlite/`
- `cache/`

Mac 上建議重新登入 Codex。

## 路徑對照

### Windows

```text
C:\Users\<你的帳號>\.codex
```

### Mac

```text
~/.codex
```

## 建議同步的內容

從 Windows 複製這些：

```text
config.toml
rules/
skills/
memories/
```

放到 Mac 的：

```text
~/.codex/
```

## 不建議同步的內容

以下請不要直接搬去 Mac：

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

## 建議流程

### 1. Windows 先整理可攜資料

在 Windows PowerShell：

```powershell
$portable = "$HOME\\Desktop\\codex-portable"
New-Item -ItemType Directory -Force -Path $portable | Out-Null
Copy-Item "$HOME\\.codex\\config.toml" $portable -Force
Copy-Item "$HOME\\.codex\\rules" $portable -Recurse -Force
Copy-Item "$HOME\\.codex\\skills" $portable -Recurse -Force
Copy-Item "$HOME\\.codex\\memories" $portable -Recurse -Force
```

接著把這個資料夾傳到 Mac。

可用方式：

- AirDrop
- iCloud Drive
- Google Drive
- Git 私有 repo
- USB

### 2. Mac 建立 `.codex`

在 Mac Terminal：

```bash
mkdir -p ~/.codex
```

### 3. 把可攜資料複製到 Mac

假設你把資料放到桌面 `~/Desktop/codex-portable`：

```bash
cp ~/Desktop/codex-portable/config.toml ~/.codex/
cp -R ~/Desktop/codex-portable/rules ~/.codex/
cp -R ~/Desktop/codex-portable/skills ~/.codex/
cp -R ~/Desktop/codex-portable/memories ~/.codex/
```

### 4. 在 Mac 重新登入 Codex

這一步請在 Mac 上重新做，不要直接搬 Windows 的 `auth.json`。

## 檢查項目

在 Mac Terminal：

```bash
ls -la ~/.codex
```

你應該至少看到：

- `config.toml`
- `rules`
- `skills`
- `memories`

## 專案同步

Codex 設定之外，專案本身仍然用 `git`：

```bash
git clone https://github.com/stormcorn/NUCOSMOS_POS.git
```

之後用：

```bash
git pull
```

## 最佳做法

- 程式碼：用 `git`
- VS Code：用 `Settings Sync`
- Codex：只同步可攜設定
- Mac 上重新登入 Codex

這樣最穩，也最不容易把 Windows 本機狀態一起帶壞到 Mac。
