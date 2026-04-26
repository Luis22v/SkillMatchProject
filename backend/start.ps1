$env:DB_PASSWORD   = ""
$env:JWT_SECRET    = "skillmatch-dev-secret-key-for-local-development-only-do-not-use-in-production-2024"

Write-Host "Iniciando backend SkillMatch..." -ForegroundColor Cyan
.\mvnw.cmd spring-boot:run
