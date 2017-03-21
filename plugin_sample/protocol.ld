SECTIONS
{
	.protocol_module :
	{
		_protocol_module_start = .;
		*(.protocol_module) 
		_protocol_module_end = .;
	}
}
