--[[
--
--Filename: FileApi.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local FileController = require("web.features.files.FileController")

local FileApi = {}

function FileApi:get(request)
    if request.path == "/api/files" then
        return FileController:list(request)
    end

    if request.path == "/api/file" then
        return FileController:get(request)
    end

    if request.path == "/api/file/download" then
        return FileController:download(request)
    end
end

function FileApi:post(request)
    if request.path == "/api/file" then
        return FileController:create(request)
    elseif request.path == "/api/file/upload" then
        return FileController:upload(request)
    elseif request.path == "/api/file/copy" then
        return FileController:copy(request)
    elseif request.path == "/api/file/move" then
        return FileController:move(request)
    elseif request.path == "/api/file/rename" then
        return FileController:rename(request)
    elseif request.path == "/api/file/mkdir" then
        return FileController:createDirectory(request)
    elseif request.path == "/api/file/zip" then
        return FileController:zip(request)
    elseif request.path == "/api/file/unzip" then
        return FileController:unzip(request)
    end
end

function FileApi:put(request)
    if request.path == "/api/file" then
        return FileController:save(request)
    end
end

function FileApi:delete(request)
    if request.path == "/api/file" then
        return FileController:delete(request)
    end
end

return FileApi
