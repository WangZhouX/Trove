package io.trove.backend.entity.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 将 PostgreSQL 的 {@code jsonb} 列映射为 Java 字符串，供持久化字段显式引用。
 * Maps PostgreSQL {@code jsonb} columns to Java strings through explicit field mappings.
 */
public final class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 将非空 JSON 字符串写入 PostgreSQL 的 {@code jsonb} 列。
     * Binds a non-null JSON string for a PostgreSQL {@code jsonb} column.
     *
     * @param statement 待执行语句。 Prepared statement to bind.
     * @param index JDBC 参数序号，从 1 开始。 One-based JDBC parameter index.
     * @param value 待写入的 JSON 字符串。 JSON string to bind.
     * @param jdbcType 映射声明中的 JDBC 类型，绑定时使用 {@code OTHER}。
     *        JDBC type declared in the mapping. Binding uses {@code OTHER}.
     * @throws SQLException JDBC 绑定失败。 If JDBC parameter binding fails.
     */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String value, JdbcType jdbcType)
            throws SQLException {
        // 让 PostgreSQL 根据目标列推断 jsonb 类型，避免按 varchar 绑定。
        // Let PostgreSQL infer jsonb from the target column by binding an untyped value.
        statement.setObject(index, value, Types.OTHER);
    }

    /**
     * 按列名读取 JSON 字符串。
     * Reads a JSON string by column label.
     *
     * @param resultSet 已定位到当前行的结果集。 Result set positioned on the current row.
     * @param columnName 查询结果中的列名或别名。 Column label or alias in the result.
     * @return 数据库返回的 JSON 字符串，SQL NULL 对应 {@code null}。
     *         JSON string returned by the database, or {@code null} for SQL NULL.
     * @throws SQLException JDBC 读取失败。 If the JDBC read fails.
     */
    @Override
    public String getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getString(columnName);
    }

    /**
     * 按列序号读取 JSON 字符串。
     * Reads a JSON string by column index.
     *
     * @param resultSet 已定位到当前行的结果集。 Result set positioned on the current row.
     * @param columnIndex JDBC 列序号，从 1 开始。 One-based JDBC column index.
     * @return 数据库返回的 JSON 字符串，SQL NULL 对应 {@code null}。
     *         JSON string returned by the database, or {@code null} for SQL NULL.
     * @throws SQLException JDBC 读取失败。 If the JDBC read fails.
     */
    @Override
    public String getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return resultSet.getString(columnIndex);
    }

    /**
     * 按 JDBC 输出参数序号读取 JSON 字符串。
     * Reads a JSON string by JDBC output parameter index.
     *
     * @param statement 已执行且包含输出参数的调用语句。 Executed callable statement with an output parameter.
     * @param columnIndex JDBC 输出参数序号，从 1 开始。 One-based JDBC output parameter index.
     * @return 数据库返回的 JSON 字符串，SQL NULL 对应 {@code null}。
     *         JSON string returned by the database, or {@code null} for SQL NULL.
     * @throws SQLException JDBC 读取失败。 If the JDBC read fails.
     */
    @Override
    public String getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return statement.getString(columnIndex);
    }
}
