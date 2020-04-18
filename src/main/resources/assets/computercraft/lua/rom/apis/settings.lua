--- The settings API allows to store values and save them to a file for
-- persistent configurations for CraftOS and your programs.
--
-- By default, the settings API will load its configuration from the
-- `/.settings` file. One can then use @{settings.save} to update the file.
--
-- @module settings

local expect = dofile("rom/modules/main/cc/expect.lua")
local type, expect, field = type, expect.expect, expect.field

local details, values = {}, {}

local function reserialize(value)
    if type(value) ~= "table" then return value end
    return textutils.unserialize(textutils.serialize(value))
end

local function copy(value)
    if type(value) ~= "table" then return value end
    local result = {}
    for k, v in pairs(value) do result[k] = copy(v) end
    return result
end

--- Register a new setting, optional specifying various properties about it.
--
-- While settings do not have to be added before being used, doing so allows
-- you to provide defaults and additional metadata.
--
-- @tparam string name The name of this option
-- @tparam[opt] { description? = string, default? = value } options Options for
-- this setting.
function add(name, options)
    expect(1, name, "string")
    expect(2, options, "table", nil)

    if options then
        options = {
            description = field(options, "description", "string", "nil"),
            default = reserialize(field(options, "default", "number", "string", "boolean", "table", "nil")),
        }
    else
        options = {}
    end

    details[name] = options
end

--- Set the value of a setting.
--
-- @tparam string name The name of the setting to set
-- @param value The setting's value. This cannot be `nil`, and must be
-- serialisable by @{textutils.serialize}.
-- @throws If this value cannot be serialised
-- @see settings.unset
function set(name, value)
    expect(1, name, "string")
    expect(2, value, "number", "string", "boolean", "table")
    values[name] = reserialize(value)
end

--- Get the value of a setting.
--
-- @tparam string name The name of the setting to get.
-- @param[opt] default The value to use should there be pre-existing value for
-- this setting. If not given, it will use the setting's default value if given,
-- or `nil` otherwise.
-- @return The setting's, or the default if the setting has not been changed.
function get(name, default)
    expect(1, name, "string")
    local result = values[name]
    if result ~= nil then
        return copy(result)
    elseif default ~= nil then
        return default
    else
        local opt = details[name]
        return opt and copy(opt.default)
    end
end

--- Get details about a specific setting
function getDetails(name)
    expect(1, name, "string")
    local deets = copy(details[name]) or {}
    deets.value = values[name]
    deets.changed = deets.value ~= nil
    if deets.value == nil then deets.value = deets.default end
    return deets
end

--- Remove the value of a setting, setting it to the default.
--
-- @{settings.get} will return the default value until the setting's value is
-- @{settings.set|set}, or the computer is rebooted.
--
-- @tparam string name The name of the setting to unset.
-- @see settings.set
-- @see settings.clear
function unset(name)
    expect(1, name, "string")
    values[name] = nil
end

--- Resets the value of all settings. Equivalent to calling @{settings.unset}
--- on every setting.
--
-- @see settings.unset
function clear()
    values = {}
end

--- Get the names of all currently defined settings.
--
-- @treturn { string } An alphabetically sorted list of all currently-defined
-- settings.
function getNames()
    local result, n = {}, 1
    for k in pairs(details) do
        result[n], n = k, n + 1
    end
    for k in pairs(values) do
        if not details[k] then result[n], n = k, n + 1 end
    end
    table.sort(result)
    return result
end

--- Load settings from the given file.
--
-- Existing settings will be merged with any pre-existing ones. Conflicting
-- entries will be overwritten, but any others will be preserved.
--
-- @tparam[opt] string sPath The file to load from, defaulting to `.settings`.
-- @treturn boolean Whether settings were successfully read from this
-- file. Reasons for failure may include the file not existing or being
-- corrupted.
--
-- @see settings.save
function load(sPath)
    expect(1, sPath, "string", "nil")
    local file = fs.open(sPath or ".settings", "r")
    if not file then
        return false
    end

    local sText = file.readAll()
    file.close()

    local tFile = textutils.unserialize(sText)
    if type(tFile) ~= "table" then
        return false
    end

    for k, v in pairs(tFile) do
        if type(k) == "string" and
           (type(v) == "string" or type(v) == "number" or type(v) == "boolean" or type(v) == "table") then
            set(k, v)
        end
    end

    return true
end

--- Save settings to the given file.
--
-- This will entirely overwrite the pre-existing file. Settings defined in the
-- file, but not currently loaded will be removed.
--
-- @tparam[opt] string sPath The path to save settings to, defaulting to `.settings`.
-- @treturn boolean If the settings were successfully saved.
--
-- @see settings.load
function save(sPath)
    expect(1, sPath, "string", "nil")
    local file = fs.open(".settings", "w")
    if not file then
        return false
    end

    file.write(textutils.serialize(values))
    file.close()

    return true
end
