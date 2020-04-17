
local tArgs = { ... }

-- Get all the files in the directory
local sDir
local bShowHidden
if #tArgs == 0 then
    sDir = shell.dir()
elseif string.sub(tArgs[1],1,1) == "-" then
    local sArgs = string.sub(tArgs[1],2,-1)
    for c in sArgs:gmatch"." do
        if c == "a" then
            bShowHiddenArg = true
        elseif c == "l" then
            bShowAttr = true
        else
            printError("Unknown Argument")
            return
        end
    end
    if tArgs[2] == nil then
        sDir = shell.dir()
    else
        sDir = shell.resolve( tArgs[1] )
    end
else
    sDir = shell.resolve( tArgs[1] )
end

if not fs.isDir( sDir ) then
    printError( "Not a directory" )
    return
end

-- Sort into dirs/files, and calculate column count
local tAll = fs.list( sDir )
local tFiles = {}
local tDirs = {}

local bShowHidden = settings.get( "list.show_hidden" ) or bShowHiddenArg
for _, sItem in pairs( tAll ) do
    if bShowHidden or string.sub( sItem, 1, 1 ) ~= "." then
        local sPath = fs.combine( sDir, sItem )
        if fs.isDir( sPath ) then
            table.insert( tDirs, sItem )
        else
            table.insert( tFiles, sItem )
        end
    end
end
table.sort( tDirs )
table.sort( tFiles )

local function writeFileTable( tPrint, tList )
    for i, v in ipairs( tList ) do
        local tAttr = fs.attributes( shell.resolve( v ) )
        local tSingleEntry = {}
        local sDate = settings.get( "list.date_string", "%d.%m.%Y" )
        for col in string.gmatch( settings.get( "list.columns", "Name:Type:Size" ), "[^:]+" ) do
            if col:lower() == "name" then
                table.insert( tSingleEntry, v )
            elseif col:lower() == "type" then
                if tAttr["isDir"] then
                    table.insert( tSingleEntry, "Dir" )
                else
                    table.insert( tSingleEntry, "File" )
                end
            elseif col:lower() == "size" then
                table.insert( tSingleEntry, tostring( tAttr["size"] ) )
            elseif col:lower() == "created" then
                table.insert( tSingleEntry, os.date( sDate, tAttr["created"] ) )
            elseif col:lower() == "modified" then
                table.insert( tSingleEntry, os.date( sDate, tAttr["modification"] ) )
            end
        end
        table.insert( tPrint, tSingleEntry )
    end
    return tPrint
end

if bShowAttr then
    local tHeader = {}
    for str in string.gmatch( settings.get( "list.columns", "Name:Type:Size" ), "[^:]+" ) do
        str = str:lower()
        str = str:gsub( "^%l", string.upper )
        if str == "Name" or str == "Type" or str == "Size" or str == "Created" or str == "Modified" then 
            table.insert( tHeader, str )
        else
            printError( 'Unknow value "' .. str .. '" for list.columns. Valid values are Name, Type, Size, Created and Modified.' )
            return
        end
    end
    local tPrint = { tHeader }
    tPrint = writeFileTable( tPrint, tDirs )
    tPrint = writeFileTable( tPrint, tFiles )
    textutils.pagedTabulate( table.unpack( tPrint ) )
else
    if term.isColour() then
        textutils.pagedTabulate( colors.green, tDirs, colors.white, tFiles )
    else
        textutils.pagedTabulate( tDirs, tFiles )
    end
end
