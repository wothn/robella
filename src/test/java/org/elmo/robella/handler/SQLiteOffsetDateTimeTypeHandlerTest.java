package org.elmo.robella.handler;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SQLiteOffsetDateTimeTypeHandlerTest {

    private SQLiteOffsetDateTimeTypeHandler handler;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private CallableStatement callableStatement;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new SQLiteOffsetDateTimeTypeHandler();
    }

    @Test
    void testSetNonNullParameter() throws SQLException {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 10, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        // ISO 8601 format: "2023-10-01T12:00:00Z" (includes seconds even if zero)
        String expectedString = "2023-10-01T12:00:00Z";

        handler.setNonNullParameter(preparedStatement, 1, offsetDateTime, JdbcType.VARCHAR);

        verify(preparedStatement).setString(1, expectedString);
    }

    @Test
    void testGetNullableResultByColumnName() throws SQLException {
        String columnName = "created_at";
        String dateTimeString = "2023-10-01T12:00:00Z";
        OffsetDateTime expectedDateTime = OffsetDateTime.parse(dateTimeString);

        when(resultSet.getString(columnName)).thenReturn(dateTimeString);

        OffsetDateTime result = handler.getNullableResult(resultSet, columnName);

        assertNotNull(result);
        assertEquals(expectedDateTime, result);
    }

    @Test
    void testGetNullableResultByColumnNameWhenNull() throws SQLException {
        String columnName = "created_at";

        when(resultSet.getString(columnName)).thenReturn(null);

        OffsetDateTime result = handler.getNullableResult(resultSet, columnName);

        assertNull(result);
    }

    @Test
    void testGetNullableResultByColumnIndex() throws SQLException {
        int columnIndex = 1;
        String dateTimeString = "2023-10-01T12:00:00Z";
        OffsetDateTime expectedDateTime = OffsetDateTime.parse(dateTimeString);

        when(resultSet.getString(columnIndex)).thenReturn(dateTimeString);

        OffsetDateTime result = handler.getNullableResult(resultSet, columnIndex);

        assertNotNull(result);
        assertEquals(expectedDateTime, result);
    }

    @Test
    void testGetNullableResultByColumnIndexWhenNull() throws SQLException {
        int columnIndex = 1;

        when(resultSet.getString(columnIndex)).thenReturn(null);

        OffsetDateTime result = handler.getNullableResult(resultSet, columnIndex);

        assertNull(result);
    }

    @Test
    void testGetNullableResultFromCallableStatement() throws SQLException {
        int columnIndex = 1;
        String dateTimeString = "2023-10-01T12:00:00Z";
        OffsetDateTime expectedDateTime = OffsetDateTime.parse(dateTimeString);

        when(callableStatement.getString(columnIndex)).thenReturn(dateTimeString);

        OffsetDateTime result = handler.getNullableResult(callableStatement, columnIndex);

        assertNotNull(result);
        assertEquals(expectedDateTime, result);
    }

    @Test
    void testGetNullableResultFromCallableStatementWhenNull() throws SQLException {
        int columnIndex = 1;

        when(callableStatement.getString(columnIndex)).thenReturn(null);

        OffsetDateTime result = handler.getNullableResult(callableStatement, columnIndex);

        assertNull(result);
    }

    @Test
    void testGetNullableResultWithSQLiteDateTimeFormat() throws SQLException {
        String columnName = "created_at";
        // SQLite datetime('now') returns format like: "2023-10-01 12:00:00"
        String dateTimeString = "2023-10-01 12:00:00";

        when(resultSet.getString(columnName)).thenReturn(dateTimeString);

        OffsetDateTime result = handler.getNullableResult(resultSet, columnName);

        assertNotNull(result);
        // Should parse as UTC when no timezone specified
        assertEquals(2023, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(12, result.getHour());
        assertEquals(ZoneOffset.UTC, result.getOffset());
    }
}