package com.mtpnda.biocue.core.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DetectionDatabase_Impl extends DetectionDatabase {
  private volatile SensorDataDao _sensorDataDao;

  private volatile EventDao _eventDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `sensor_windows` (`timestamp` INTEGER NOT NULL, `hrMean` REAL NOT NULL, `rmssd` REAL NOT NULL, `accMag` REAL NOT NULL, `repetitiveness` REAL NOT NULL, `hrSlope` REAL NOT NULL, `isOffBody` INTEGER NOT NULL, `isExercising` INTEGER NOT NULL, PRIMARY KEY(`timestamp`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stress_events` (`id` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `triggerType` TEXT NOT NULL, `label` TEXT NOT NULL, `isResolved` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8fe0de95431a1e4dcb1a41e33b9ddb3a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `sensor_windows`");
        db.execSQL("DROP TABLE IF EXISTS `stress_events`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSensorWindows = new HashMap<String, TableInfo.Column>(8);
        _columnsSensorWindows.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSensorWindows.put("hrMean", new TableInfo.Column("hrMean", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSensorWindows.put("rmssd", new TableInfo.Column("rmssd", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSensorWindows.put("accMag", new TableInfo.Column("accMag", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSensorWindows.put("repetitiveness", new TableInfo.Column("repetitiveness", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSensorWindows.put("hrSlope", new TableInfo.Column("hrSlope", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSensorWindows.put("isOffBody", new TableInfo.Column("isOffBody", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSensorWindows.put("isExercising", new TableInfo.Column("isExercising", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSensorWindows = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSensorWindows = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSensorWindows = new TableInfo("sensor_windows", _columnsSensorWindows, _foreignKeysSensorWindows, _indicesSensorWindows);
        final TableInfo _existingSensorWindows = TableInfo.read(db, "sensor_windows");
        if (!_infoSensorWindows.equals(_existingSensorWindows)) {
          return new RoomOpenHelper.ValidationResult(false, "sensor_windows(com.mtpnda.biocue.core.data.local.SensorWindowEntity).\n"
                  + " Expected:\n" + _infoSensorWindows + "\n"
                  + " Found:\n" + _existingSensorWindows);
        }
        final HashMap<String, TableInfo.Column> _columnsStressEvents = new HashMap<String, TableInfo.Column>(6);
        _columnsStressEvents.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStressEvents.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStressEvents.put("endTime", new TableInfo.Column("endTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStressEvents.put("triggerType", new TableInfo.Column("triggerType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStressEvents.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStressEvents.put("isResolved", new TableInfo.Column("isResolved", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStressEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStressEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStressEvents = new TableInfo("stress_events", _columnsStressEvents, _foreignKeysStressEvents, _indicesStressEvents);
        final TableInfo _existingStressEvents = TableInfo.read(db, "stress_events");
        if (!_infoStressEvents.equals(_existingStressEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "stress_events(com.mtpnda.biocue.core.data.local.StressEventEntity).\n"
                  + " Expected:\n" + _infoStressEvents + "\n"
                  + " Found:\n" + _existingStressEvents);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "8fe0de95431a1e4dcb1a41e33b9ddb3a", "55103980b2e168a4a43d74b82abe447b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "sensor_windows","stress_events");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `sensor_windows`");
      _db.execSQL("DELETE FROM `stress_events`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(SensorDataDao.class, SensorDataDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EventDao.class, EventDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public SensorDataDao sensorDataDao() {
    if (_sensorDataDao != null) {
      return _sensorDataDao;
    } else {
      synchronized(this) {
        if(_sensorDataDao == null) {
          _sensorDataDao = new SensorDataDao_Impl(this);
        }
        return _sensorDataDao;
      }
    }
  }

  @Override
  public EventDao eventDao() {
    if (_eventDao != null) {
      return _eventDao;
    } else {
      synchronized(this) {
        if(_eventDao == null) {
          _eventDao = new EventDao_Impl(this);
        }
        return _eventDao;
      }
    }
  }
}
