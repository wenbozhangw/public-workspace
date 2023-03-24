function clear(rec, binNames)
    if not aerospike:exists(rec) then
        return
    end

    if list.size(binNames) == 0 then
        warn("Execute bin name clear illegal args, bin name list has no data!")
        return
    end

    local updateCount = 0;
    for binName in list.iterator(binNames) do
        warn("bin name : " .. binName)
        if binName == nil or binName == '' then
            warn("Skip blank bin name!")
        elseif rec[binName] ~= nil then
            rec[binName] = nil
            updateCount = updateCount + 1;
        end
    end

    if updateCount > 0 then
        aerospike:updateCount(rec);
    end
end