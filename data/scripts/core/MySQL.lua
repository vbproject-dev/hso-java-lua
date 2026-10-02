--[[
--
--Filename: MySQL.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:48 am
--
--Author: VBPROJECT
--
--]]

local MySQL   = {}
MySQL.__index = MySQL

local Query   = {}
Query.__index = Query


local _instance = nil


function MySQL.connect(host, user, password, database, port)
    if _instance then return _instance end

    assert(type(host) == "string", "host must be a string")
    assert(type(user) == "string", "user must be a string")
    assert(type(password) == "string", "password must be a string")
    assert(type(database) == "string", "database must be a string")

    local conn, err = MysqlConnection.createConnection(host, user, password, database, port or 3306)
    if not conn then return nil, err end

    _instance = setmetatable({ _conn = conn }, MySQL)
    return _instance
end

function MySQL.instance()
    assert(_instance, "MySQL not connected, call MySQL.connect() first")
    return _instance
end

function MySQL:close()
    if not self._conn then return false, "not connected" end
    self._conn:close()
    self._conn = nil
    _instance = nil
    return true
end

function MySQL:ping()
    if not self._conn then return false, "not connected" end
    return self._conn:ping()
end

function MySQL:from(table)
    assert(type(table) == "string" and #table > 0, "table name must be a non-empty string")
    assert(self._conn, "not connected")

    return setmetatable({
        _conn = self._conn,
        _table = table,
        _wheres = {},
        _order = nil,
        _limit = nil,
        _cols = "*",
        _class = nil,
    }, Query)
end

-- ─── Query Builder ─────────────────────────────────────────────────────────────

function Query:select(...)
    local cols = { ... }
    assert(#cols > 0, "select requires at least one column")
    self._cols = table.concat(cols, ", ")
    return self
end

function Query:class(class)
    assert(type(class) == "table", "class must be a table")
    assert(type(class.new) == "function", "class.new() is required")

    self._class = class
    return self
end

function Query:_mapRow(row)
    if self._class then
        return self._class.new(row)
    end

    return row
end

function Query:_mapRows(rows)
    if not self._class then
        return rows
    end

    for i = 1, #rows do
        rows[i] = self:_mapRow(rows[i])
    end

    return rows
end

function Query:where(column, operator, value)
    if value == nil then
        value = operator
        operator = "="
    end

    assert(type(column) == "string" and #column > 0)
    assert(type(operator) == "string")
    assert(value ~= nil)

    local operators = {
        ["="] = true,
        [">"] = true,
        ["<"] = true,
        [">="] = true,
        ["<="] = true,
        ["!="] = true,
        ["<>"] = true
    }

    assert(operators[operator], "invalid where operator")

    if type(value) == "string" then
        value = "'" .. value .. "'"
    elseif type(value) == "number" then
        value = tostring(value)
    elseif type(value) == "boolean" then
        value = value and "1" or "0"
    else
        error("unsupported where value type: " .. type(value))
    end

    table.insert(self._wheres, column .. " " .. operator .. " " .. value)

    return self
end

function Query:orderBy(column, direction)
    assert(type(column) == "string" and #column > 0, "orderBy: column must be a non-empty string")
    direction = (direction or "ASC"):upper()
    assert(direction == "ASC" or direction == "DESC", "orderBy: direction must be ASC or DESC")
    self._order = column .. " " .. direction
    return self
end

function Query:limit(n)
    assert(type(n) == "number" and n > 0 and math.floor(n) == n, "limit must be a positive integer")
    self._limit = n
    return self
end

function Query:_buildSelect()
    local sql = string.format("SELECT %s FROM %s", self._cols, self._table)
    if #self._wheres > 0 then
        sql = sql .. " WHERE " .. table.concat(self._wheres, " AND ")
    end
    if self._order then sql = sql .. " ORDER BY " .. self._order end
    if self._limit then sql = sql .. " LIMIT " .. self._limit end
    return sql
end

function Query:getAll()
    local sql = self:_buildSelect()

    local ok, err = self._conn:query(sql)
    if not ok then
        return nil, err
    end

    local rows, err = self._conn:fetchAll()
    if not rows then
        return nil, err
    end

    return self:_mapRows(rows)
end

function Query:getFirst()
    self._limit = 1

    local rows, err = self:getAll()
    if not rows then
        return nil, err
    end

    return rows[1]
end

function Query:insert(data)
    local ok, result = xpcall(function()
        assert(type(data) == "table" and next(data) ~= nil, "insert: data must be a non-empty table")

        local cols, vals = {}, {}

        for k, v in pairs(data) do
            assert(type(k) == "string", "insert: column name must be a string")
            table.insert(cols, k)

            if type(v) == "string" then
                table.insert(vals, string.format("'%s'", v))
            elseif type(v) == "number" then
                table.insert(vals, tostring(v))
            elseif type(v) == "boolean" then
                table.insert(vals, v and "1" or "0")
            elseif v == nil then
                table.insert(vals, "NULL")
            else
                error("insert: unsupported value type for column '" .. k .. "': " .. type(v))
            end
        end

        local sql = string.format("INSERT INTO %s (%s) VALUES (%s)",
            self._table,
            table.concat(cols, ", "),
            table.concat(vals, ", "))

        local success, err = self._conn:query(sql)
        if not success then
            error(err)
        end

        return self._conn:insertId()
    end, debug.traceback)

    if not ok then
        return nil, result
    end

    return result, nil
end

function Query:update(data)
    local ok, result = xpcall(function()
        assert(type(data) == "table" and next(data) ~= nil, "update: data must be a non-empty table")
        assert(#self._wheres > 0, "update: at least one where() condition is required")

        local sets = {}

        for k, v in pairs(data) do
            assert(type(k) == "string", "update: column name must be a string")

            if type(v) == "string" then
                table.insert(sets, string.format("%s = '%s'", k, v))
            elseif type(v) == "number" then
                table.insert(sets, string.format("%s = %s", k, tostring(v)))
            elseif type(v) == "boolean" then
                table.insert(sets, string.format("%s = %s", k, v and "1" or "0"))
            elseif v == nil then
                table.insert(sets, string.format("%s = NULL", k))
            else
                error("update: unsupported value type for column '" .. k .. "': " .. type(v))
            end
        end

        local sql = string.format("UPDATE %s SET %s WHERE %s",
            self._table,
            table.concat(sets, ", "),
            table.concat(self._wheres, " AND "))

        local success, err = self._conn:query(sql)
        if not success then
            error(err)
        end

        return self._conn:affectedRows()
    end, debug.traceback)

    if not ok then
        return nil, result
    end

    return result, nil
end

function Query:delete()
    local ok, result = xpcall(function()
        assert(#self._wheres > 0, "delete: at least one where() condition is required to prevent full table wipe")

        local sql = string.format("DELETE FROM %s WHERE %s",
            self._table,
            table.concat(self._wheres, " AND "))

        local success, err = self._conn:query(sql)
        if not success then
            error(err)
        end

        return self._conn:affectedRows()
    end, debug.traceback)

    if not ok then
        return nil, result
    end

    return result, nil
end

function Query:count()
    self._cols = "COUNT(*) as count"
    local row, err = self:getFirst()
    if not row then return nil, err end
    return tonumber(row.count) or 0
end

return MySQL
