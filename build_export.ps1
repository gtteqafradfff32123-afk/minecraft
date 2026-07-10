$out = "F:\open\export_for_review\ALL_SOURCE_CODE.md"
$files = Get-ChildItem "F:\open\export_for_review" -Recurse -File -Include "*.java","*.json","*.toml","*.gradle","*.properties","*.md","*.txt","*.mcmeta","*.mixins.json","*.lang" | Where-Object { $_.FullName -notmatch "ALL_SOURCE_CODE" -and $_.FullName -notmatch "node_modules" -and $_.FullName -notmatch "\.class$" } | Sort-Object FullName

$bt = "`"
foreach ($f in $files) {
    $rel = $f.FullName.Replace("F:\open\export_for_review\", "")
    $ext = $f.Extension.TrimStart('.')
    if ($ext -eq '') { $ext = 'txt' }
    $header = "`n" + $bt + $bt + $bt + $ext + "`n// " + $rel + "`n"
    $footer = "`n" + $bt + $bt + $bt
    Add-Content -Path $out -Value $header
    Get-Content $f.FullName -Raw | Add-Content -Path $out
    Add-Content -Path $out -Value $footer
}
$size = [math]::Round((Get-Item $out).Length / 1MB, 2)
$size