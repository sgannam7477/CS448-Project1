# Roles

Shrenick Gannamani: Buffer Manage
Dhruv Chanana: Heap Files
Vivan Gupta: Heap Scans

# HeapFile
Dhruv Chanana

## Functionality
The HeapFile class provides basic functionality for insertion, updating,
deleting, and selecting (or getting?) records. If the HeapFile is constructed
with an empty name, it's considered a temporary HeapFile, and will be deleted
when the object leaves memory.

## Complexity Requirements
Used TreeMap in order to ensure $O(log(n))$ insertion, updating, and deletion
for records of some given size. Pages are also stored within a doubly linked list
through HFPage's next and previous page pointers, which allows for easy
reconstruction of the HeapFile when an existing one is opened and easy scanning. In order to get constant time
for all other functions, we keep a hashset that keeps track of all pages that
are currently part of the file, because the linkedlist alone is not enough to
achieve constant time in this case.

# HeapScan
Vivan Gupta

## Functionality
The HeapScan class takes advantage of the doubly linked list implemented by
HFPage in order to iterate through the HeapFile. Internally, we store the
current page and RID of the current record. These can be accessed through
getNext() until the Heapfile no longer has any records.
