--[[
--
--Filename: FileController.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local FileService = require("web.features.files.FileService")

local FileController = {}

local MIME_TYPES = {
    html = "text/html; charset=utf-8",
    htm  = "text/html; charset=utf-8",
    css  = "text/css; charset=utf-8",
    js   = "text/javascript",
    json = "application/json",
    txt  = "text/plain; charset=utf-8",
    xml  = "application/xml",
    csv  = "text/csv; charset=utf-8",
    png  = "image/png",
    jpg  = "image/jpeg",
    jpeg = "image/jpeg",
    gif  = "image/gif",
    webp = "image/webp",
    svg  = "image/svg+xml",
    ico  = "image/x-icon",
    pdf  = "application/pdf",
    zip  = "application/zip",
    mp3  = "audio/mpeg",
    wav  = "audio/wav",
    mp4  = "video/mp4",
    webm = "video/webm"
}

function FileController:list(request)
    local path = "."

    if request.query and request.query.path then
        path = request.query.path
    end

    local files, err = FileService:list(path)

    if not files then
        return { error = err }
    end

    return files
end

function FileController:get(request)
    local path = request.query and request.query.path

    if not path then
        return { error = "Missing path" }
    end

    local content, err = FileService:read(path)

    if not content then
        return { error = err }
    end

    local extension = FileService:getExtension(path)

    if extension == "json" then
        local data, parseErr = JSON.toTable(content)

        if not data then
            return { error = parseErr or "Invalid JSON" }
        end

        content = data
    end

    return {
        path = path,
        content = content
    }
end

function FileController:create(request)
    local data = JSON.toTable(request.body)

    if not data or not data.path then
        return { error = "Invalid request" }
    end

    local ok, err =
        FileService:create(
            data.path,
            data.content or ""
        )

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = data.path
    }
end

function FileController:save(request)
    local data = JSON.toTable(request.body)

    if not data or not data.path then
        return { error = "Invalid request" }
    end

    local ok, err =
        FileService:write(
            data.path,
            data.content or ""
        )

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = data.path
    }
end

function FileController:delete(request)
    local path = request.query and request.query.path

    if not path then
        return { error = "Missing path" }
    end

    local recursive = FileService:isDirectory(path)
    local ok, err = File.remove(path, recursive)

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = path
    }
end

function FileController:rename(request)
    local data = JSON.toTable(request.body)

    if not data or not data.path or not data.name then
        return { error = "Invalid request" }
    end

    local path = data.path
    local slash = path:match("^.*()/")

    local destination

    if slash then
        destination = path:sub(1, slash) .. data.name
    else
        destination = data.name
    end

    local ok, err = FileService:rename(path, destination)

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = destination
    }
end

function FileController:createDirectory(request)
    local data = JSON.toTable(request.body)

    if not data or not data.path then
        return { error = "Invalid request" }
    end

    local ok, err =
        FileService:createDirectory(data.path)

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = data.path
    }
end

function FileController:copy(request)
    local data = JSON.toTable(request.body)

    if not data or not data.source or not data.destination then
        return { error = "Invalid request" }
    end

    local ok, err =
        FileService:copy(
            data.source,
            data.destination
        )

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = data.destination
    }
end

function FileController:move(request)
    local data = JSON.toTable(request.body)

    if not data or not data.source or not data.destination then
        return { error = "Invalid request" }
    end

    local ok, err = FileService:move(data.source, data.destination)

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = data.destination
    }
end

function FileController:zip(request)
    local data = JSON.toTable(request.body)

    if not data or not data.source or not data.destination then
        return { error = "Invalid request" }
    end

    local path, err = FileService:zip(data.source, data.destination)

    if not path then
        return { error = err }
    end

    return {
        success = true,
        path = path
    }
end

function FileController:unzip(request)
    local data = JSON.toTable(request.body)

    if not data or not data.source or not data.destination then
        return { error = "Invalid request" }
    end

    local path, err = FileService:unzip(data.source, data.destination)

    if not path then
        return { error = err }
    end

    return {
        success = true,
        path = path
    }
end

function FileController:upload(request)
    local uploaded = request.files and request.files[1]

    if not uploaded then
        return { error = "Missing file" }
    end

    local path = request.fields and request.fields.path

    if not path then
        return { error = "Missing path" }
    end

    local ok, err = FileService:write(path, uploaded.data)

    if not ok then
        return { error = err }
    end

    return {
        success = true,
        path = path
    }
end

function FileController:download(request)
    local path = request.query and request.query.path

    if not path then
        return { error = "Missing path" }
    end

    local content, err = FileService:read(path)

    if not content then
        return { error = err }
    end

    local name = path:match("([^/]+)$") or path
    local extension = FileService:getExtension(path)
    local mime = (extension and MIME_TYPES[extension])
        or "application/octet-stream"

    return {
        filename = name,
        contentType = mime,
        data = content
    }
end

return FileController
