box.cfg{
    listen = 3301,
    log_level = 5
}

local kv = box.schema.space.create('KV', {if_not_exists = true})

kv:format({
    {name = 'key',   type = 'string'},
    {name = 'value', type = 'varbinary', is_nullable = true}
})

kv:create_index('primary', {
    type  = 'TREE',
    parts = {'key'},
    if_not_exists = true
})

box.schema.user.grant('guest', 'read,write,execute', 'universe',
        nil, {if_not_exists = true})
