package com.mtpnda.biocue.core.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SensorDataDao_Impl implements SensorDataDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SensorWindowEntity> __insertionAdapterOfSensorWindowEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public SensorDataDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSensorWindowEntity = new EntityInsertionAdapter<SensorWindowEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sensor_windows` (`timestamp`,`hrMean`,`rmssd`,`accMag`,`repetitiveness`,`hrSlope`,`isOffBody`,`isExercising`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SensorWindowEntity entity) {
        statement.bindLong(1, entity.getTimestamp());
        statement.bindDouble(2, entity.getHrMean());
        statement.bindDouble(3, entity.getRmssd());
        statement.bindDouble(4, entity.getAccMag());
        statement.bindDouble(5, entity.getRepetitiveness());
        statement.bindDouble(6, entity.getHrSlope());
        final int _tmp = entity.isOffBody() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final int _tmp_1 = entity.isExercising() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sensor_windows WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SensorWindowEntity window,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSensorWindowEntity.insert(window);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long thresholdTime,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, thresholdTime);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecentWindows(final int limit,
      final Continuation<? super List<SensorWindowEntity>> $completion) {
    final String _sql = "SELECT * FROM sensor_windows ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SensorWindowEntity>>() {
      @Override
      @NonNull
      public List<SensorWindowEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfHrMean = CursorUtil.getColumnIndexOrThrow(_cursor, "hrMean");
          final int _cursorIndexOfRmssd = CursorUtil.getColumnIndexOrThrow(_cursor, "rmssd");
          final int _cursorIndexOfAccMag = CursorUtil.getColumnIndexOrThrow(_cursor, "accMag");
          final int _cursorIndexOfRepetitiveness = CursorUtil.getColumnIndexOrThrow(_cursor, "repetitiveness");
          final int _cursorIndexOfHrSlope = CursorUtil.getColumnIndexOrThrow(_cursor, "hrSlope");
          final int _cursorIndexOfIsOffBody = CursorUtil.getColumnIndexOrThrow(_cursor, "isOffBody");
          final int _cursorIndexOfIsExercising = CursorUtil.getColumnIndexOrThrow(_cursor, "isExercising");
          final List<SensorWindowEntity> _result = new ArrayList<SensorWindowEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SensorWindowEntity _item;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpHrMean;
            _tmpHrMean = _cursor.getDouble(_cursorIndexOfHrMean);
            final double _tmpRmssd;
            _tmpRmssd = _cursor.getDouble(_cursorIndexOfRmssd);
            final double _tmpAccMag;
            _tmpAccMag = _cursor.getDouble(_cursorIndexOfAccMag);
            final double _tmpRepetitiveness;
            _tmpRepetitiveness = _cursor.getDouble(_cursorIndexOfRepetitiveness);
            final double _tmpHrSlope;
            _tmpHrSlope = _cursor.getDouble(_cursorIndexOfHrSlope);
            final boolean _tmpIsOffBody;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOffBody);
            _tmpIsOffBody = _tmp != 0;
            final boolean _tmpIsExercising;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsExercising);
            _tmpIsExercising = _tmp_1 != 0;
            _item = new SensorWindowEntity(_tmpTimestamp,_tmpHrMean,_tmpRmssd,_tmpAccMag,_tmpRepetitiveness,_tmpHrSlope,_tmpIsOffBody,_tmpIsExercising);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
