package net.interlecto.proho.shared

/** A command tried to change or link to a record that has been archived. */
class RecordArchivedException : RuntimeException("The record is archived")
