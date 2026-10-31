# Console Weather Forecasting System - Setup Guide

Java 17 | Maven | MySQL 8 | Eclipse

**Both data sources are used:**
- Option 1 fetches a live 7-day forecast from Open-Meteo (free, no API key) and saves it to MySQL.
- Option 2 reads saved forecasts from MySQL (works offline). If the internet fails during option 1, the app falls back to saved data automatically.

## 1. Prerequisites
| Tool | Version |
|------|---------|
| JDK | 17+ (check: `java -version`) |
| Eclipse IDE for Java Developers | 2023-09 or newer (includes Maven/m2e) |
| MySQL Server + Workbench | 8.x |
| Internet | needed for option 1 and the first Maven build |

## 2. Create the database
1. Open **MySQL Workbench** and connect to your local server (as `root`).
2. **File > Open SQL Script...** and choose `sql/weather_db.sql`.
3. Click the lightning-bolt **Execute** button.
4. The result grid at the bottom should show sample forecasts for 5 cities.

The script creates database `weather_db`, user `weather_user` / password `Weather@123`, three tables (`cities`, `forecasts`, `search_history`) and sample data.

## 3. Import into Eclipse
1. Unzip `weather-forecast-system.zip`.
2. **File > Import... > Maven > Existing Maven Projects > Next**.
3. Browse to the unzipped `weather-forecast-system` folder, tick `pom.xml`, click **Finish**.
4. Wait for Maven to download dependencies (progress bar bottom-right).
5. Confirm Java 17: right-click project > **Properties > Java Compiler** should show 17. Otherwise **Window > Preferences > Java > Installed JREs** and add/select JDK 17, then **Maven > Update Project...**.

## 4. Configure the connection
Open `src/main/resources/db.properties`. If you used the script unchanged, nothing to edit. To use root instead:
```
db.user=root
db.password=YOUR_ROOT_PASSWORD
```

## 5. Run
- Right-click `src/main/java/com/weather/Main.java` > **Run As > Java Application**.
- Type your choices in the Eclipse **Console** view.

Optional, from a terminal:
```
mvn clean package
java -jar target/weather-forecast-system-1.0.0.jar
```

## 6. Try it
1. Option `2`, enter `Chennai` - sample data from MySQL.
2. Option `1`, enter `Coimbatore` - live data fetched and saved.
3. Turn off Wi-Fi, option `1` for `Coimbatore` - falls back to saved data.
4. Option `4` - search history.

## Troubleshooting
| Problem | Fix |
|---------|-----|
| `Access denied for user` | Re-run the SQL script or fix user/password in `db.properties`. |
| `Communications link failure` | MySQL not running, or port is not 3306. |
| `release version 17 not supported` | Eclipse/Maven is using an older JDK (step 3.5). |
| Dependencies not downloading | Right-click project > **Maven > Update Project...** (tick Force Update). |
| `[Online lookup failed]` | No internet/firewall; saved data is used instead. |
| Saved data "out of date" | Sample data covers 7 days from the day you ran the SQL; use option 1 to refresh. |

## Project layout
```
pom.xml
sql/weather_db.sql
src/main/resources/db.properties
src/main/java/com/weather/
  Main.java
  model/    City, Forecast, WeatherCodes
  db/       Database (JDBC), WeatherDao (all SQL)
  service/  WeatherApiService (Open-Meteo HTTP + JSON)
  ui/       ConsoleApp (menu)
```
